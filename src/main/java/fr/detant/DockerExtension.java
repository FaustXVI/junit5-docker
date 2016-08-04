package fr.detant;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static com.github.dockerjava.core.DockerClientBuilder.getInstance;
import static com.github.dockerjava.core.DockerClientConfig.createDefaultConfigBuilder;

public class DockerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerExtension.class);

    private final DockerClient dockerClient;

    private CreateContainerResponse container;

    public DockerExtension() {
        dockerClient = getInstance(createDefaultConfigBuilder().withApiVersion("1.22")).build();
    }

    @Override
    public void beforeAll(ContainerExtensionContext containerExtensionContext) throws Exception {
        Docker dockerAnnotation = containerExtensionContext.getTestClass()
                .orElseThrow(() -> new IllegalStateException("Test should be ran in a class"))
                .getAnnotation(Docker.class);
        String imageName = dockerAnnotation.image();
        Port[] ports = dockerAnnotation.ports();
        ArrayList<ExposedPort> exposedPorts = new ArrayList<>();
        Ports portBindings = new Ports();
        for (Port port : ports) {
            ExposedPort innerPort = tcp(port.inner());
            exposedPorts.add(innerPort);
            portBindings.bind(innerPort, bindPort(port.exposed()));
        }
        container = dockerClient.createContainerCmd(imageName)
                .withExposedPorts(exposedPorts)
                .withPortBindings(portBindings)
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        LOGGER.info("Started container {} with image {}", container.getId(), imageName);
    }

    @Override
    public void afterAll(ContainerExtensionContext containerExtensionContext) throws Exception {
        dockerClient.stopContainerCmd(container.getId()).exec();
        dockerClient.removeContainerCmd(container.getId()).exec();
    }
}
