package com.github.junit5docker;

import com.github.junit5docker.fakes.FakeContainerExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.mockito.ArgumentCaptor;

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

import static com.github.junit5docker.ExecutorSanitizer.ignoreInterrupted;
import static com.github.junit5docker.WaitFor.NOTHING;
import static com.github.junit5docker.assertions.CountDownLatchAssertions.assertThat;
import static com.github.junit5docker.assertions.ThreadedAssertions.assertExecutionOf;
import static com.github.junit5docker.fakes.FakeLog.fakeLog;
import static com.github.junit5docker.fakes.FakeLog.unfoundableLog;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerExtensionTest {

    static final String WAITED_LOG = "started";

    private DockerClientAdapter dockerClient = mock(DockerClientAdapter.class);

    private DockerExtension dockerExtension = new DockerExtension(dockerClient);

    @Nested
    class BeforeAllTestsShould {

        @Test
        public void startContainerWithOnePort() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OnePortTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(),
                eq(new PortBinding(8801, 8800)));
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
            when(dockerClient.logs(anyString())).thenReturn(Stream.generate(ignoreInterrupted(() -> {
                TimeUnit.MILLISECONDS.sleep(100);
                return null;
            })));
            dockerExtension.beforeAll(context);
        }

        @Test
        public void startContainerWithEnvironmentVariables() {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OneEnvironmentTest.class);
            dockerExtension.beforeAll(context);
            ArgumentCaptor<Map<String, String>> mapArgumentCaptor = getMapArgumentCaptor();
            verify(dockerClient).startContainer(eq("wantedImage"),
                mapArgumentCaptor.capture(), any());
            Map<String, String> environment = mapArgumentCaptor.getValue();
            assertThat(environment)
                .hasSize(1)
                .containsKeys("toTest")
                .containsValues("myValue");
        }

        @SuppressWarnings("unchecked")
        private ArgumentCaptor<Map<String, String>> getMapArgumentCaptor() {
            return ArgumentCaptor.forClass(Map.class);
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
                CompletableFuture<Void> voidCompletableFuture = runAsync(ignoreInterrupted(() -> {
                    assertThat(logRequest)
                        .overridingErrorMessage("should have ask for logs")
                        .isDownBefore(500, TimeUnit.MILLISECONDS);
                    mainThread.interrupt();
                }));
                dockerExtension.beforeAll(context);
                assertThat(Thread.interrupted())
                    .overridingErrorMessage("Interrupted thread should still interrupted")
                    .isTrue();
                assertExecutionOf(voidCompletableFuture::get).hasNoAssertionFailures();
            }

            private long sendLogAndTimeExecution(int waitingTime, TimeUnit timeUnit, Runnable runnable)
                throws InterruptedException, ExecutionException {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                long callTime = System.currentTimeMillis();
                Future<?> logSent = sendLogAfter(waitingTime, timeUnit, executor);
                runnable.run();
                long duration = System.currentTimeMillis() - callTime;
                shutDown(executor, waitingTime, timeUnit);
                assertExecutionOf(logSent::get).hasNoAssertionFailures();
                return duration;
            }

            private void shutDown(ExecutorService executor, int waitingTime, TimeUnit timeUnit)
                throws InterruptedException {
                executor.shutdown();
                assertThat(executor.awaitTermination(waitingTime * 2, timeUnit))
                    .overridingErrorMessage("execution should have finished")
                    .isTrue();
            }

            private Future<?> sendLogAfter(int waitingTime, TimeUnit timeUnit, ExecutorService executor) {
                AtomicBoolean sendLog = new AtomicBoolean(false);
                Stream<String> logStream = fakeLog(sendLog, WAITED_LOG);
                when(dockerClient.logs(argThat(argument -> true))).thenReturn(logStream);
                return executor.submit(ignoreInterrupted(() -> {
                    timeUnit.sleep(waitingTime);
                    sendLog.set(true);
                }));
            }
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
    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800))
    private static class OnePortTest {

    }

    @Docker(image = "wantedImage", ports = {@Port(exposed = 8801, inner = 8800), @Port(exposed = 9901, inner = 9900)})
    private static class MultiplePortTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        environments = @Environment(key = "toTest", value = "myValue"))
    private static class OneEnvironmentTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800), waitFor = @WaitFor(WAITED_LOG))
    private static class WaitForLogTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        waitFor = @WaitFor(value = WAITED_LOG, timeoutInMillis = 10))
    private static class TimeoutTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        waitFor = @WaitFor(value = NOTHING, timeoutInMillis = 10))
    private static class WaitForNothingTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        waitFor = @WaitFor(value = "unfoundable log", timeoutInMillis = 200))
    private static class WaitForNotPresentLogTest {

    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        waitFor = @WaitFor(value = "unfoundable log", timeoutInMillis = 2000))
    private static class InterruptionTest {

    }
}
