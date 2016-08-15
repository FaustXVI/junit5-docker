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

import static com.github.dockerjava.core.DockerClientConfig.createDefaultConfigBuilder;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Default docker client's ")
public class DefaultDockerClientIT {

    private static final String WANTED_IMAGE = "faustxvi/simple-two-ports:latest";

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

            @BeforeEach
            public void ensureImageIsPulled() {
                ensureImageExists();
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

        private String containerID;

        @BeforeEach
        public void startAContainer() {
            ensureImageExists();
            containerID = dockerClient.createContainerCmd(WANTED_IMAGE).exec().getId();
            dockerClient.startContainerCmd(containerID).exec();
        }

        @Test
        public void shouldRemoveTheContainer() {
            defaultDockerClient.stopAndRemoveContainer(containerID);
            assertEquals(containers.size(), dockerClient.listContainersCmd().exec().size());
            assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(containerID).exec());
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

    private void ensureImageExists() {
        try {
            dockerClient.inspectImageCmd(WANTED_IMAGE).exec();
        } catch (NotFoundException e) {
            dockerClient.pullImageCmd(WANTED_IMAGE).exec(new PullImageResultCallback()).awaitSuccess();
        }
    }
}