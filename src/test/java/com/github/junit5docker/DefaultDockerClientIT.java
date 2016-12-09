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

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.dockerjava.core.DockerClientConfig.createDefaultConfigBuilder;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
                assertThat(dockerClient.listContainersCmd().exec()).hasSize(containers.size() + 1);
                InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                assertThat(startedContainer.getConfig().getImage()).isEqualTo(WANTED_IMAGE);
            }

            @Test
            @DisplayName("start a container with one port")
            public void shouldStartContainerWithOnePort() {
                String containerId = defaultDockerClient.startContainer(WANTED_IMAGE, emptyMap(), new PortBinding
                        (8081, 8080));
                InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                Ports ports = startedContainer.getHostConfig().getPortBindings();
                assertThat(ports).isNotNull();
                Map<ExposedPort, Ports.Binding[]> portBindings = ports.getBindings();
                assertThat(portBindings).hasSize(1)
                        .containsKeys(new ExposedPort(8080));
                assertThat(portBindings.get(new ExposedPort(8080))).hasSize(1)
                        .extracting(Ports.Binding::getHostPortSpec)
                        .contains("8081");
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
                assertThat(envs).hasSize(2 + DEFAULT_DOCKER_ENV_NUMBER)
                        .contains("khaled=souf", "abdellah=stagiaire");
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
                assertThat(dockerClient.listContainersCmd().exec()).hasSize(containers.size() + 1);
                InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                assertThat(startedContainer.getConfig().getImage()).isEqualTo(WANTED_IMAGE);
            }

            @Nested
            @DisplayName("with a bug in docker-java should")
            class WithABugInDockerJava {

                @Test
                @DisplayName("add latest to the image name if none is given")
                public void shouldStartLatestContainer() {
                    String containerId = defaultDockerClient.startContainer("faustxvi/simple-two-ports", emptyMap());
                    List<Container> currentContainers = dockerClient.listContainersCmd().exec();
                    assertThat(currentContainers).hasSize(containers.size() + 1);
                    InspectContainerResponse startedContainer = dockerClient.inspectContainerCmd(containerId).exec();
                    assertThat(startedContainer.getConfig().getImage()).isEqualTo(WANTED_IMAGE);
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
            assertThat(dockerClient.listContainersCmd().exec()).hasSize(containers.size());
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(() -> dockerClient.inspectContainerCmd(containerID).exec());
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
                containerID = dockerClient.createContainerCmd(WANTED_IMAGE).withEnv(singletonList("WAITING_TIME=1ms"))
                        .exec()
                        .getId();
                dockerClient.startContainerCmd(containerID).exec();
                Stream<String> logs = defaultDockerClient.logs(containerID);
                Optional<String> firstLine = logs.findFirst();
                assertThat(firstLine).isPresent()
                .hasValueSatisfying("started"::equals);
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
            public void shouldCloseWhenContainerCloses() throws InterruptedException {
                Stream<String> logs = defaultDockerClient.logs(containerID);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                CountDownLatch streamReadStarted = new CountDownLatch(1);
                CountDownLatch streamClosed = new CountDownLatch(1);
                executor
                        .submit(() -> {
                            logs.peek((t) -> streamReadStarted.countDown())
                                    .filter((l) -> false).findFirst();
                            streamClosed.countDown();
                        });
                try {
                    streamReadStarted.await();
                    assertThat(streamClosed.await(1, TimeUnit.SECONDS))
                            .overridingErrorMessage("Log stream should have been closed")
                            .isTrue();
                } finally {
                    executor.shutdown();
                }
            }
        }
    }
}