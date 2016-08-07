package fr.detant;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ContainerExtensionContext;

public class DockerExtension implements BeforeAllCallback, AfterAllCallback {

    private final DockerClientAdapter dockerClient;

    private String containerID;

    public DockerExtension() {
        this(new DefaultDockerClient());
    }

    public DockerExtension(DockerClientAdapter dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void beforeAll(ContainerExtensionContext containerExtensionContext) throws Exception {
        Class<?> testClass = containerExtensionContext.getTestClass().get();
        Docker dockerAnnotation = testClass.getAnnotation(Docker.class);
        Port[] ports = dockerAnnotation.ports();
        PortBinding[] portBindings = new PortBinding[ports.length];
        for (int i = 0; i < ports.length; i++) {
            Port port = ports[i];
            portBindings[i] = new PortBinding(port.exposed(), port.inner());
        }
        containerID = dockerClient.startContainer(dockerAnnotation.image(), portBindings);
    }

    @Override
    public void afterAll(ContainerExtensionContext containerExtensionContext) throws Exception {
        dockerClient.stopAndRemoveContainer(containerID);
    }
}
