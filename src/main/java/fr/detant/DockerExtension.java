package fr.detant;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerExtension.class);

    private final DockerClient dockerClient;

    private CreateContainerResponse container;

    public DockerExtension() {
        dockerClient = DockerClientBuilder.getInstance(
                DockerClientConfig.createDefaultConfigBuilder()
                        .withApiVersion("1.22")
                        .build())
                .build();
    }

    @Override
    public void beforeAll(ContainerExtensionContext containerExtensionContext) throws Exception {
        Docker dockerAnnotation = containerExtensionContext.getTestClass()
                .orElseThrow(() -> new IllegalStateException("Test should be ran in a class"))
                .getAnnotation(Docker.class);
        String imageName = dockerAnnotation.image();
        int exposedPort = Integer.parseInt(dockerAnnotation.ports().split(":")[0]);
        int innerPort = Integer.parseInt(dockerAnnotation.ports().split(":")[1]);
        ExposedPort tcp22 = ExposedPort.tcp(innerPort);
        Ports portBindings = new Ports();
        portBindings.bind(tcp22, Ports.Binding.bindPort(exposedPort));
        container = dockerClient.createContainerCmd(imageName)
                .withExposedPorts(tcp22)
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
