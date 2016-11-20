package com.github.junit5docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.junit.jupiter.api.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.github.dockerjava.core.DockerClientConfig.createDefaultConfigBuilder;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Default docker client's ")
public class DefaultDockerClientIT {

    public static final int DEFAULT_DOCKER_ENV_NUMBER = 1;

    private DefaultDockerClient defaultDockerClient = new DefaultDockerClient();

    private DockerClient dockerClient = DockerClientBuilder.getInstance(createDefaultConfigBuilder().withApiVersion("1.22")).build();

    private List<Container> containers;

    @BeforeEach
    public void getExistingContainers() {
        containers = dockerClient.listContainersCmd().exec();
    }

    @Nested
    @DisplayName("startContainer method")
    class StartContainerMethod {

        @Nested
        @DisplayName("with image already pulled should")
        class WithImageAlreadyPulled {
            private static final String WANTED_IMAGE = "faustxvi/simple-two-ports:latest";

            @BeforeEach
            public void ensureImageIsPulled() {
                ensureImageExists(WANTED_IMAGE);
            }

            @Test
            @DisplayName("start a container without ports")
            public void shouldStartContainer() {
                String containerId = defaultDockerClient.startContainer(WANTED_IMAGE, emptyMap());
                assertEquals(containers.size() + 1, dockerClient.listContainersCmd().exec().size());
                InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                assertEquals(WANTED_IMAGE, startedContainer.getConfig().getImage());
            }

            @Test
            @DisplayName("start a container with one port")
            public void shouldStartContainerWithOnePort() {
                String containerId = defaultDockerClient.startContainer(WANTED_IMAGE, emptyMap(), new PortBinding
                        (8081, 8080));
                InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                Ports ports = startedContainer.getHostConfig().getPortBindings();
                assertNotNull(ports);
                Map<ExposedPort, Ports.Binding[]> portBindings = ports.getBindings();
                assertEquals(1, portBindings.size());
                assertEquals(8080, new ArrayList<>(portBindings.keySet()).get(0).getPort());
                assertEquals(1, portBindings.get(new ExposedPort(8080)).length);
                assertEquals("8081", portBindings.get(new ExposedPort(8080))[0].getHostPortSpec());
            }

            @Test
            @DisplayName("start a container with environment variables >:)")
            public void shouldStartContainerWithEnvironmentVariables() {
                Map<String, String> environments = new HashMap<>();
                environments.put("khaled", "souf");
                environments.put("abdellah", "stagiaire");
                String containerId = defaultDockerClient.startContainer(WANTED_IMAGE, environments);
                InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                List<String> envs = Arrays.asList(startedContainer.getConfig().getEnv());
                assertEquals(2 + DEFAULT_DOCKER_ENV_NUMBER, envs.size());
                assertTrue(envs.contains("khaled=souf"));
                assertTrue(envs.contains("abdellah=stagiaire"));
            }
        }

        @Nested
        @DisplayName("with image not pulled should")
        class WithImageNotPulled {
            private static final String WANTED_IMAGE = "faustxvi/simple-two-ports:latest";

            @BeforeEach
            public void ensureContainerIsNotPresent() {
                try {
                    String imageToRemove = dockerClient.inspectImageCmd(WANTED_IMAGE).exec().getId();
                    dockerClient.removeImageCmd(imageToRemove).exec();
                } catch (NotFoundException e) {
                    // not found, no problems
                }
            }

            @Test
            @DisplayName("start a container after pulling the image")
            public void shouldStartContainer() {
                String containerId = defaultDockerClient.startContainer(WANTED_IMAGE, emptyMap());
                assertEquals(containers.size() + 1, dockerClient.listContainersCmd().exec().size());
                InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                assertEquals(WANTED_IMAGE, startedContainer.getConfig().getImage());
            }

            @Nested
            @DisplayName("with a bug in docker-java should")
            class WithABugInDockerJava {

                @Test
                @DisplayName("add latest to the image name if none is given")
                public void shouldStartLatestContainer() {
                    String containerId = defaultDockerClient.startContainer("faustxvi/simple-two-ports", emptyMap());
                    List<Container> currentContainers = dockerClient.listContainersCmd().exec();
                    assertEquals(containers.size() + 1, currentContainers.size());
                    InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                    assertEquals(WANTED_IMAGE, startedContainer.getConfig().getImage());
                }
            }
        }
    }

    @Nested
    @DisplayName("stopAndRemove method")
    class StopAndRemoveContainerMethod {
        private static final String WANTED_IMAGE = "faustxvi/simple-two-ports:latest";

        private String containerID;

        @BeforeEach
        public void startAContainer() {
            ensureImageExists(WANTED_IMAGE);
            containerID = dockerClient.createContainerCmd(WANTED_IMAGE).exec().getId();
            dockerClient.startContainerCmd(containerID).exec();
        }

        @Test
        @DisplayName("should remove the container")
        public void shouldRemoveTheContainer() {
            defaultDockerClient.stopAndRemoveContainer(containerID);
            assertEquals(containers.size(), dockerClient.listContainersCmd().exec().size());
            assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(containerID).exec());
        }
    }

    @Nested
    @DisplayName("log method")
    class LogMethod {

        @Nested
        @DisplayName("with a working image")
        class WithAWorkingImage {
            private static final String WANTED_IMAGE = "faustxvi/open-port-later";

            private String containerID;

            @BeforeEach
            public void startAContainer() {
                ensureImageExists(WANTED_IMAGE);
            }

            @Test
            public void shouldGiveLogsInStream() {
                startContainerWithWaitingTimeAt("1ms");
                Stream<String> logs = defaultDockerClient.logs(containerID);
                Optional<String> firstLine = logs.findFirst();
                assertTrue(firstLine.isPresent());
                assertThat(firstLine.get()).contains("started");
            }

            @Test
            @DisplayName("should close stream if interrupted")
            public void shouldCloseIfInterrupted() {
                startContainerWithWaitingTimeAt("1s");
                ExecutorService executorService = interruptIn(100, TimeUnit.MILLISECONDS, currentThread());
                try {
                    Stream<String> logs = defaultDockerClient.logs(containerID);
                    Optional<String> notFoundLine = logs.filter((l) -> false).findFirst();
                    Thread.interrupted();
                    assertFalse(notFoundLine.isPresent());
                } finally {
                    executorService.shutdown();
                }
            }

            private void startContainerWithWaitingTimeAt(String duration) {
                containerID = dockerClient.createContainerCmd(WANTED_IMAGE).withEnv(singletonList("WAITING_TIME=" + duration))
                        .exec()
                        .getId();
                dockerClient.startContainerCmd(containerID).exec();
            }
        }

        @Nested
        @DisplayName("with a buggy image")
        class WithABuggyImage {

            private static final String WANTED_IMAGE = "faustxvi/log-and-quit";

            private String containerID;

            @BeforeEach
            public void startAContainer() {
                ensureImageExists(WANTED_IMAGE);
                containerID = dockerClient.createContainerCmd(WANTED_IMAGE)
                        .exec()
                        .getId();
                dockerClient.startContainerCmd(containerID).exec();
            }

            @Test
            @DisplayName("should close stream when logs finish")
            public void shouldCloseWhenContainerCloses() {
                Stream<String> logs = defaultDockerClient.logs(containerID);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Boolean> lineFound = executor
                        .submit(() -> logs.filter((l) -> false).findFirst().isPresent());
                try {
                    assertFalse(lineFound.get(1, TimeUnit.SECONDS));
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    StringWriter out = new StringWriter();
                    e.printStackTrace(new PrintWriter(out));
                    String stack = out.toString();
                    fail("Log stream should have been close but reading the logs didn't ended.\nException : " + stack);
                } finally {
                    executor.shutdown();
                }
            }
        }
    }

    @AfterEach
    public void stopAndRemoveStartedContainers() {
        dockerClient.listContainersCmd().exec().stream()
                .filter(container -> !containers.contains(container))
                .forEach(container -> {
                    dockerClient.stopContainerCmd(container.getId()).exec();
                    dockerClient.removeContainerCmd(container.getId()).exec();
                });
    }

    private void ensureImageExists(String wantedImage) {
        try {
            dockerClient.inspectImageCmd(wantedImage).exec();
        } catch (NotFoundException e) {
            dockerClient.pullImageCmd(wantedImage).exec(new PullImageResultCallback()).awaitSuccess();
        }
    }

    private ExecutorService interruptIn(int timeout, TimeUnit timeUnit, Thread thread) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                timeUnit.sleep(timeout);
                thread.interrupt();
            } catch (InterruptedException e) {
                thread.interrupt();
            }
        });
        return executorService;
    }
}