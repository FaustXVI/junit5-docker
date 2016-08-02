package fr.detant;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.TestExtensionContext;

public class DockerExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

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
    public void beforeTestExecution(TestExtensionContext testExtensionContext) throws Exception {
        Docker dockerAnnotation = testExtensionContext.getTestInstance().getClass().getAnnotation(Docker.class);
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
    }

    @Override
    public void afterTestExecution(TestExtensionContext testExtensionContext) throws Exception {
        dockerClient.stopContainerCmd(container.getId()).exec();
    }
}
