package com.github.junit5docker;

import com.github.junit5docker.fakes.FakeExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DockerExtensionTest {

    private static final String WAITED_LOG = "started";

    private static final String CONTAINER_ID = "CONTAINER_ID";

    private static final ContainerInfo CONTAINER_INFO = new ContainerInfo(CONTAINER_ID, null);

    private final DockerClientAdapter dockerClient = mock(DockerClientAdapter.class);

    private final DockerExtension dockerExtension = new DockerExtension(dockerClient);

    @Nested
    class BeforeEachTestsShould {

        @Test
        void startContainerNotMarked() {
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
            ExtensionContext context = new FakeExtensionContext(DefaultCreationContainerTest.class);
            dockerExtension.beforeEach(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(), any(), eq(new PortBinding(8801, 8800)));
        }

        @Test
        void notStartContainerIfMarkedAsReused() {
            ExtensionContext context = new FakeExtensionContext(DoNotRecreateContainerTest.class);
            dockerExtension.beforeEach(context);
            verify(dockerClient, never()).startContainer(any(), anyMap(), any());
        }
    }

    @Nested
    class BeforeAllTestsShould {

        @Captor
        private ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

        @Captor
        private ArgumentCaptor<String[]> stringArrayArgumentCaptor;

        @BeforeEach
        void initMocks() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        void startContainerWithOnePort() {
            ExtensionContext context = new FakeExtensionContext(OnePortTest.class);
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(), any(), eq(new PortBinding(8801, 8800)));
        }

        @Test
        void notStartContainerIfMarkedAsRecreated() {
            ExtensionContext context = new FakeExtensionContext(DefaultCreationContainerTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient, never()).startContainer(any(), anyMap(), any());
        }

        @Test
        void startContainerWithMultiplePorts() {
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
            ExtensionContext context = new FakeExtensionContext(MultiplePortTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"),
                                                anyMap(),
                                                any(),
                                                eq(new PortBinding(8801, 8800)),
                                                eq(new PortBinding(9901, 9900)));
        }

        @Test
        void notWaitByDefault() {
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
            ExtensionContext context = new FakeExtensionContext(WaitForNothingTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient, never()).logs(anyString());
        }

        @Test
        void startContainerWithEnvironmentVariables() {
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
            ExtensionContext context = new FakeExtensionContext(OneEnvironmentTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), mapArgumentCaptor.capture(), any(), any());
            Map<String, String> environment = mapArgumentCaptor.getValue();
            assertThat(environment).hasSize(1).containsKeys("toTest").containsValues("myValue");
        }

        @Test
        void startContainerWithNetworks() {
            List<String> networkIds = Arrays.asList("my-network-1", "my-network-2");
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(new ContainerInfo(
                CONTAINER_ID,
                networkIds));
            ExtensionContext context = new FakeExtensionContext(TwoNetworksTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), any(), stringArrayArgumentCaptor.capture(), any());
            String[] networks = stringArrayArgumentCaptor.getValue();
            assertThat(networks).containsExactly("my-network-1", "my-network-2");
        }

        @Nested
        class BeThreadSafe {

            @Test
            void waitForLogToAppear() throws ExecutionException, InterruptedException {
                when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
                ExtensionContext context = new FakeExtensionContext(WaitForLogTest.class);
                long duration = sendLogAndTimeExecution(100,
                    TimeUnit.MILLISECONDS,
                    () -> dockerExtension.beforeAll(context));
                assertThat(duration).overridingErrorMessage(
                    "Should have waited for log to appear during %d ms but waited %d ms",
                    100,
                    duration).isGreaterThanOrEqualTo(100);
            }

            @Test
            void closeLogStreamOnceFound() {
                when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
                AtomicBoolean streamClosed = new AtomicBoolean(false);
                Stream<String> logStream = Stream.of(WAITED_LOG).onClose(() -> streamClosed.set(true));
                when(dockerClient.logs(argThat(argument -> true))).thenReturn(logStream);
                dockerExtension.beforeAll(new FakeExtensionContext(WaitForLogTest.class));
                assertThat(streamClosed.get()).as("Stream should be closed").isTrue();
            }

            @Test
            void closeLogEvenWithExceptionOnRead() {
                when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
                AtomicBoolean streamClosed = new AtomicBoolean(false);
                Stream<String> logStream = Stream.<String>generate(() -> {
                    throw new RuntimeException();
                })
                    .onClose(() -> streamClosed.set(true));
                when(dockerClient.logs(argThat(argument -> true))).thenReturn(logStream);
                assertThatThrownBy(() -> dockerExtension.beforeAll(new FakeExtensionContext(WaitForLogTest.class)));
                assertThat(streamClosed.get()).as("Stream should be closed").isTrue();
            }

            @Test
            void timeoutIfLogDoesNotAppear() {
                when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
                assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
                    ExtensionContext context = new FakeExtensionContext(TimeoutTest.class);
                    sendLogAndTimeExecution(1, TimeUnit.SECONDS, () -> dockerExtension.beforeAll(context));
                }).withMessageContaining("Timeout");
            }

            @Test
            void throwsExceptionIfLogNotFoundAndLogsEnded() {
                when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
                assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
                    ExtensionContext context = new FakeExtensionContext(WaitForNotPresentLogTest.class);
                    sendLogAndTimeExecution(100, TimeUnit.MILLISECONDS, () -> dockerExtension.beforeAll(context));
                }).withMessageContaining("not found");
            }

            @Test
            void beInterruptible() throws ExecutionException, InterruptedException {
                when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
                ExtensionContext context = new FakeExtensionContext(InterruptionTest.class);
                Thread mainThread = Thread.currentThread();
                CountDownLatch logRequest = new CountDownLatch(1);
                when(dockerClient.logs(argThat(argument -> true))).thenAnswer(mock -> {
                    logRequest.countDown();
                    return unfoundableLog();
                });
                CompletableFuture<Void> voidCompletableFuture = runAsync(() -> {
                    assertThat(logRequest).overridingErrorMessage("should have ask for logs")
                                          .isDownBefore(500, TimeUnit.MILLISECONDS);
                    mainThread.interrupt();
                });
                dockerExtension.beforeAll(context);
                assertThat(Thread.interrupted()).overridingErrorMessage("Interrupted thread should still interrupted")
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

        private final FakeExtensionContext defaultContext = new FakeExtensionContext(DefaultCreationContainerTest
                                                                                         .class);

        @BeforeEach
        void callBefore() {
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
            dockerExtension.beforeEach(defaultContext);
        }

        @Test
        void stopContainer() {
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
            dockerExtension.afterEach(defaultContext);
            verify(dockerClient).stopAndRemoveContainer(CONTAINER_ID);
        }

        @Test
        void notStopContainerNotMarkedAsRenewable() {
            ExtensionContext context = new FakeExtensionContext(OnePortTest.class);
            dockerExtension.afterEach(context);
            verify(dockerClient, never()).stopAndRemoveContainer(any());
        }

    }

    @Nested
    class AfterAllTestsShould {

        private final FakeExtensionContext onePortExtensionContext = new FakeExtensionContext(OnePortTest.class);

        @BeforeEach
        void callBefore() {
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(CONTAINER_INFO);
            dockerExtension.beforeAll(onePortExtensionContext);
        }

        @Test
        void stopContainer() {
            dockerExtension.afterAll(onePortExtensionContext);
            verify(dockerClient).stopAndRemoveContainer(CONTAINER_ID);
        }

        @Test
        void notStopContainerMarkedAsRenewable() {
            ExtensionContext context = new FakeExtensionContext(DefaultCreationContainerTest.class);
            dockerExtension.afterAll(context);
            verify(dockerClient, never()).stopAndRemoveContainer(any());
        }

        @Test
        void disconnectFromAndRemoveNetworks() {
            ExtensionContext context = new FakeExtensionContext(TwoNetworksTest.class);
            List<String> networkIds = Arrays.asList("111", "222");
            when(dockerClient.startContainer(anyString(), anyMap(), any(), any())).thenReturn(new ContainerInfo(
                CONTAINER_ID,
                networkIds));
            dockerExtension.beforeAll(context);
            dockerExtension.afterAll(context);
            verify(dockerClient, times(2)).disconnectFromNetwork(eq(CONTAINER_ID), argThat(networkIds::contains));
            verify(dockerClient, times(2)).maybeRemoveNetwork(any());
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

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
        networks = {"my-network-1", "my-network-2"}, newForEachCase = false)
    private static class TwoNetworksTest {

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
