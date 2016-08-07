package fr.detant;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static com.github.dockerjava.core.DockerClientConfig.createDefaultConfigBuilder;

class DefaultDockerClient implements DockerClientAdapter {

    private final DockerClient dockerClient;

    DefaultDockerClient() {
        dockerClient = DockerClientBuilder.getInstance(createDefaultConfigBuilder().withApiVersion("1.22")).build();
    }

    @Override
    public String startContainer(String wantedImage, PortBinding... portBinding) {
        if(!wantedImage.contains(":")){
            wantedImage += ":latest";
        }
        this.ensureImageExists(wantedImage);
        Ports bindings = new Ports();
        for (PortBinding binding : portBinding) {
            ExposedPort inner = tcp(binding.inner);
            bindings.bind(inner, bindPort(binding.exposed));
        }
        CreateContainerResponse containerResponse = dockerClient.createContainerCmd(wantedImage)
                .withPortBindings(bindings)
                .exec();
        dockerClient.startContainerCmd(containerResponse.getId()).exec();
        return containerResponse.getId();
    }

    @Override
    public void stopAndRemoveContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).exec();
    }

    private void ensureImageExists(String wantedImage) {
        try {
            dockerClient.inspectImageCmd(wantedImage).exec();
        } catch (NotFoundException e) {
            dockerClient.pullImageCmd(wantedImage).exec(new PullImageResultCallback()).awaitSuccess();
        }
    }
}
