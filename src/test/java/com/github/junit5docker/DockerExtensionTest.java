package com.github.junit5docker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class DockerExtensionTest {

    static final String WAITED_LOG = "started";

    private DockerClientAdapter dockerClient = mock(DockerClientAdapter.class);

    private DockerExtension dockerExtension = new DockerExtension(dockerClient);

    @Nested
    class BeforeAllTestsShould {

        @Test
        public void startContainerWithOnePort() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OnePortTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(), eq(new PortBinding(8801, 8800)));
        }

        @Test
        public void startContainerWithMultiplePorts() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(MultiplePortTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(), eq(new PortBinding(8801, 8800)),
                    eq(new PortBinding(9901, 9900)));
        }

        @Test
        public void waitForLogToAppear() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(WaitForLogTest.class);
            long duration = sendLogAndTimeExecution(100, TimeUnit.MILLISECONDS, () -> dockerExtension.beforeAll(context));
            assertThat(duration)
                    .overridingErrorMessage("Should have waited for log to appear during %d ms but waited %d ms", 100,
                            duration)
                    .isGreaterThanOrEqualTo(100);
        }

        @Test
        public void timeoutIfLogDoesNotAppear() throws Exception {
            expectThrows(AssertionError.class, () -> {
                ContainerExtensionContext context = new FakeContainerExtensionContext(TimeoutTest.class);
                sendLogAndTimeExecution(1, TimeUnit.SECONDS, () -> dockerExtension.beforeAll(context));
            });
        }

        private long sendLogAndTimeExecution(int waitingTime, TimeUnit timeUnit, Runnable runnable) throws InterruptedException {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            long callTime = System.currentTimeMillis();
            sendLogAfter(waitingTime, timeUnit, executor);
            runnable.run();
            long duration = System.currentTimeMillis() - callTime;
            executor.shutdown();
            if (!executor.awaitTermination(waitingTime * 2, timeUnit)) {
                fail("execution should have finished");
            }
            return duration;
        }

        private void sendLogAfter(int waitingTime, TimeUnit timeUnit, ExecutorService executor) {
            AtomicBoolean started = new AtomicBoolean(false);
            when(dockerClient.logs(anyString())).thenReturn(Stream.generate(() -> {
                if (started.get()) return "19/11/2016 : " + WAITED_LOG;
                return "";
            }));
            executor.submit(() -> {
                try {
                    timeUnit.sleep(waitingTime);
                    started.set(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        @Test
        public void startContainerWithEnvironmentVariables() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OneEnvironmentTest.class);
            dockerExtension.beforeAll(context);
            ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dockerClient).startContainer(eq("wantedImage"),
                    mapArgumentCaptor.<String, String>capture(), any(PortBinding[].class));
            Map<String, String> environment = mapArgumentCaptor.getValue();
            assertEquals(1, environment.size());
            assertTrue(environment.containsKey("toTest"));
            assertEquals("myValue", environment.get("toTest"));
        }
    }

    @Nested
    class AfterAllTestsShould {

        private static final String CONTAINER_ID = "CONTAINER_ID";

        @BeforeEach
        public void callBefore() throws Exception {
            when(dockerClient.startContainer(anyString(), Mockito.<String, String>anyMap(),
                    any(PortBinding[].class)))
                    .thenReturn(CONTAINER_ID);
            dockerExtension.beforeAll(new FakeContainerExtensionContext(OnePortTest.class));
        }

        @Test
        public void stopContainer() throws Exception {
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
}