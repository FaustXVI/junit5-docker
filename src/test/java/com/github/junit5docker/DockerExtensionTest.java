package com.github.junit5docker;

import com.github.junit5docker.fakes.FakeContainerExtensionContext;
import com.github.junit5docker.fakes.FakeTestExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.github.junit5docker.WaitFor.NOTHING;
import static com.github.junit5docker.assertions.CountDownLatchAssertions.assertThat;
import static com.github.junit5docker.assertions.ExecutionAssertions.assertNoInterruptionThrown;
import static com.github.junit5docker.assertions.ExecutorAssertions.assertThat;
import static com.github.junit5docker.assertions.ThreadedAssertions.assertExecutionOf;
import static com.github.junit5docker.fakes.FakeLog.fakeLog;
import static com.github.junit5docker.fakes.FakeLog.unfoundableLog;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerExtensionTest {

    static final String WAITED_LOG = "started";

    private DockerClientAdapter dockerClient = mock(DockerClientAdapter.class);

    private DockerExtension dockerExtension = new DockerExtension(dockerClient);

    @Nested
    class BeforeEachTestsShould {

        @Test
        public void startContainerNotMarked() {
            TestExtensionContext context = new FakeTestExtensionContext(DefaultCreationContainerTest.class);
            dockerExtension.beforeEach(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(),
                eq(new PortBinding(8801, 8800)));
        }

        @Test
        public void notStartContainerIfMarkedAsReused() {
            TestExtensionContext context = new FakeTestExtensionContext(DoNotRecreateContainerTest.class);
            dockerExtension.beforeEach(context);
            verify(dockerClient, never()).startContainer(any(), anyMap(), any());
        }
    }

    @Nested
    class BeforeAllTestsShould {

        @Captor
        private ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

        @BeforeEach
        public void initMocks() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void startContainerWithOnePort() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OnePortTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(),
                eq(new PortBinding(8801, 8800)));
        }

        @Test
        public void notStartContainerIfMarkedAsRecreated() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(DefaultCreationContainerTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient, never()).startContainer(any(), anyMap(), any());
        }

        @Test
        public void startContainerWithMultiplePorts() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(MultiplePortTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(),
                eq(new PortBinding(8801, 8800)),
                eq(new PortBinding(9901, 9900)));
        }

        @Test
        public void notWaitByDefault() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(WaitForNothingTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient, never()).logs(anyString());
        }

        @Test
        public void startContainerWithEnvironmentVariables() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OneEnvironmentTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"),
                mapArgumentCaptor.capture(), any());
            Map<String, String> environment = mapArgumentCaptor.getValue();
            assertThat(environment)
                .hasSize(1)
                .containsKeys("toTest")
                .containsValues("myValue");
        }

        @Nested
        class BeThreadSafe {

            @Test
            public void waitForLogToAppear() throws ExecutionException, InterruptedException {
                ContainerExtensionContext context = new FakeContainerExtensionContext(WaitForLogTest.class);
                long duration = sendLogAndTimeExecution(100,
                    TimeUnit.MILLISECONDS, () -> dockerExtension.beforeAll(context));
                assertThat(duration)
                    .overridingErrorMessage("Should have waited for log to appear during %d ms but waited %d ms", 100,
                        duration)
                    .isGreaterThanOrEqualTo(100);
            }

            @Test
            public void closeLogStreamOnceFound() throws ExecutionException, InterruptedException {
                AtomicBoolean streamClosed = new AtomicBoolean(false);
                Stream<String> logStream = Stream.of(WAITED_LOG).onClose(() -> streamClosed.set(true));
                when(dockerClient.logs(argThat(argument -> true))).thenReturn(logStream);
                dockerExtension.beforeAll(new FakeContainerExtensionContext(WaitForLogTest.class));
                assertThat(streamClosed.get()).as("Stream should be closed").isTrue();
            }

            @Test
            public void closeLogEvenWithExceptionOnRead() throws ExecutionException, InterruptedException {
                AtomicBoolean streamClosed = new AtomicBoolean(false);
                Stream<String> logStream = Stream.<String>generate(() -> {
                    throw new RuntimeException();
                })
                    .onClose(() -> streamClosed.set(true));
                when(dockerClient.logs(argThat(argument -> true))).thenReturn(logStream);
                assertThatThrownBy(
                    () -> dockerExtension.beforeAll(new FakeContainerExtensionContext(WaitForLogTest.class))
                );
                assertThat(streamClosed.get()).as("Stream should be closed").isTrue();
            }

            @Test
            public void timeoutIfLogDoesNotAppear() {
                assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
                    ContainerExtensionContext context = new FakeContainerExtensionContext(TimeoutTest.class);
                    sendLogAndTimeExecution(1, TimeUnit.SECONDS, () -> dockerExtension.beforeAll(context));
                }).withMessageContaining("Timeout");
            }

            @Test
            public void throwsExceptionIfLogNotFoundAndLogsEnded() {
                assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
                    ContainerExtensionContext context =
                        new FakeContainerExtensionContext(WaitForNotPresentLogTest.class);
                    sendLogAndTimeExecution(100, TimeUnit.MILLISECONDS,
                        () -> dockerExtension.beforeAll(context));
                }).withMessageContaining("not found");
            }

            @Test
            public void beInterruptible() throws ExecutionException, InterruptedException {
                ContainerExtensionContext context = new FakeContainerExtensionContext(InterruptionTest.class);
                Thread mainThread = Thread.currentThread();
                CountDownLatch logRequest = new CountDownLatch(1);
                when(dockerClient.logs(argThat(argument -> true))).thenAnswer(mock -> {
                    logRequest.countDown();
                    return unfoundableLog();
                });
                CompletableFuture<Void> voidCompletableFuture = runAsync(() -> {
                    assertThat(logRequest)
                        .overridingErrorMessage("should have ask for logs")
                        .isDownBefore(500, TimeUnit.MILLISECONDS);
                    mainThread.interrupt();
                });
                dockerExtension.beforeAll(context);
                assertThat(Thread.interrupted())
                    .overridingErrorMessage("Interrupted thread should still interrupted")
                    .isTrue();
                assertExecutionOf(voidCompletableFuture::get).hasNoAssertionFailures();
            }

            private long sendLogAndTimeExecution(int waitingTime, TimeUnit timeUnit, Runnable runnable)
                throws InterruptedException, ExecutionException {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                long callTime = System.nanoTime();
                Future<?> logSent = sendLogAfter(waitingTime, timeUnit, executor);
                runnable.run();
                long duration = System.nanoTime() - callTime;
                shutDown(executor, waitingTime, timeUnit);
                assertExecutionOf(logSent::get).hasNoAssertionFailures();
                return TimeUnit.NANOSECONDS.toMillis(duration);
            }

            private void shutDown(ExecutorService executor, int waitingTime, TimeUnit timeUnit)
                throws InterruptedException {
                executor.shutdown();
                assertThat(executor)
                    .overridingErrorMessage("execution should have finished")
                    .isShutedDownBefore(waitingTime * 2, timeUnit);
            }

            private Future<?> sendLogAfter(int waitingTime, TimeUnit timeUnit, ExecutorService executor) {
                AtomicBoolean sendLog = new AtomicBoolean(false);
                Stream<String> logStream = fakeLog(sendLog, WAITED_LOG);
                when(dockerClient.logs(argThat(argument -> true))).thenReturn(logStream);
                return executor.submit(assertNoInterruptionThrown(() -> {
                    timeUnit.sleep(waitingTime);
                    sendLog.set(true);
                }));
            }
        }

    }

    @Nested
    class AfterEachTestsShould {

        private static final String CONTAINER_ID = "CONTAINER_ID";

        @BeforeEach
        public void callBefore() {
            when(dockerClient.startContainer(anyString(), anyMap(),
                any()))
                .thenReturn(CONTAINER_ID);
            dockerExtension.beforeEach(new FakeTestExtensionContext(DefaultCreationContainerTest.class));
        }

        @Test
        public void stopContainer() {
            TestExtensionContext context = new FakeTestExtensionContext(DefaultCreationContainerTest.class);
            dockerExtension.afterEach(context);
            verify(dockerClient).stopAndRemoveContainer(CONTAINER_ID);
        }

        @Test
        public void notStopContainerNotMarkedAsRenewable() {
            TestExtensionContext context = new FakeTestExtensionContext(OnePortTest.class);
            dockerExtension.afterEach(context);
            verify(dockerClient, never()).stopAndRemoveContainer(any());
        }

    }

    @Nested
    class AfterAllTestsShould {

        private static final String CONTAINER_ID = "CONTAINER_ID";

        @BeforeEach
        public void callBefore() {
            when(dockerClient.startContainer(anyString(), anyMap(),
                any()))
                .thenReturn(CONTAINER_ID);
            dockerExtension.beforeAll(new FakeContainerExtensionContext(OnePortTest.class));
        }

        @Test
        public void stopContainer() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OnePortTest.class);
            dockerExtension.afterAll(context);
            verify(dockerClient).stopAndRemoveContainer(CONTAINER_ID);
        }

        @Test
        public void notStopContainerMarkedAsRenewable() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(DefaultCreationContainerTest.class);
            dockerExtension.afterAll(context);
            verify(dockerClient, never()).stopAndRemoveContainer(any());
        }
    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800), newForEachCase = false)
    private static class OnePortTest {

    }

    @Docker(image = "wantedImage", ports = {@Port(exposed = 8801, inner = 8800), @Port(exposed = 9901, inner = 9900)},
        newForEachCase = false)
    private static class MultiplePortTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        environments = @Environment(key = "toTest", value = "myValue"), newForEachCase = false)
    private static class OneEnvironmentTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800), waitFor = @WaitFor(WAITED_LOG),
        newForEachCase = false)
    private static class WaitForLogTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        waitFor = @WaitFor(value = WAITED_LOG, timeoutInMillis = 10), newForEachCase = false)
    private static class TimeoutTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        waitFor = @WaitFor(value = NOTHING, timeoutInMillis = 10), newForEachCase = false)
    private static class WaitForNothingTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        waitFor = @WaitFor(value = "unfoundable log", timeoutInMillis = 200), newForEachCase = false)
    private static class WaitForNotPresentLogTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        waitFor = @WaitFor(value = "unfoundable log", timeoutInMillis = 2000), newForEachCase = false)
    private static class InterruptionTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800))
    private static class DefaultCreationContainerTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800), newForEachCase = false)
    private static class DoNotRecreateContainerTest {

    }

}
