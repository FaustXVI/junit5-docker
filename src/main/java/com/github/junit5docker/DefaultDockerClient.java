package com.github.junit5docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static com.github.dockerjava.core.DefaultDockerClientConfig.createDefaultConfigBuilder;
import static java.util.stream.Collectors.toList;

class DefaultDockerClient implements DockerClientAdapter {

    private final DockerClient dockerClient;

    DefaultDockerClient() {
        dockerClient = DockerClientBuilder.getInstance(createDefaultConfigBuilder().withApiVersion("1.22")).build();
    }

    @Override
    public ContainerInfo startContainer(String wantedImage,
                                        Map<String, String> environment,
                                        String[] networkNames,
                                        PortBinding... portBinding) {
        Ports bindings = createPortBindings(portBinding);
        List<String> environmentStrings = createEnvironmentList(environment);
        String containerId = createContainer(wantedImage, bindings, environmentStrings);
        dockerClient.startContainerCmd(containerId).exec();
        Collection<String> networkIds = joinNetworks(networkNames, containerId);
        return new ContainerInfo(containerId, networkIds);
    }

    private Collection<String> joinNetworks(String[] networkNames, String containerId) {
        if (networkNames == null || networkNames.length == 0) {
            return Collections.emptySet();
        }
        Collection<String> networkIds = new HashSet<>();

        for (String network : networkNames) {
            String networkId = getNetworkId(network);
            dockerClient.connectToNetworkCmd()
                        .withNetworkId(networkId)
                        .withContainerId(containerId)
                        .exec();
            networkIds.add(networkId);
        }
        return networkIds;
    }

    private String getNetworkId(String networkName) {
        return getExistingNetworkId(networkName)
                   .orElseGet(() -> dockerClient.createNetworkCmd().withName(networkName).exec().getId());
    }

    private Optional<String> getExistingNetworkId(String networkName) {
        return dockerClient.listNetworksCmd()
                           .exec()
                           .stream()
                           .filter(name -> name.getName().equals(networkName))
                           .reduce((name1, name2) -> {
                               String msg = String.format("Multiple networks found with the same name: %s and %s",
                                                          name1,
                                                          name2);
                               throw new IllegalStateException(msg);
                           })
                           .map(Network::getId);
    }

    @Override
    public void disconnectFromNetwork(String containerId, String networkId) {
        dockerClient.disconnectFromNetworkCmd()
                    .withContainerId(containerId)
                    .withNetworkId(networkId)
                    .exec();
    }

    @Override
    public void maybeRemoveNetwork(String networkId) {
        if (dockerClient.inspectNetworkCmd().withNetworkId(networkId).exec().getContainers().isEmpty()) {
            dockerClient.removeNetworkCmd(networkId).exec();
        }
    }

    @Override
    public void stopAndRemoveContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).exec();
    }

    @Override
    public Stream<String> logs(String containerId) {
        return dockerClient.logContainerCmd(containerId).withFollowStream(true)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new StreamLog())
                .stream();
    }

    private String createContainer(String wantedImage, Ports bindings, List<String> environmentStrings) {
        String imageWithVersion = wantedImage;
        if (!imageWithVersion.contains(":")) {
            imageWithVersion += ":latest";
        }
        this.ensureImageExists(imageWithVersion);
        return dockerClient.createContainerCmd(imageWithVersion)
                .withEnv(environmentStrings)
                .withPortBindings(bindings)
                .exec().getId();
    }

    private List<String> createEnvironmentList(Map<String, String> environment) {
        return environment.entrySet().stream().map(this::toEnvString).collect(toList());
    }

    private Ports createPortBindings(PortBinding... portBinding) {
        Ports bindings = new Ports();
        for (PortBinding binding : portBinding) {
            ExposedPort inner = tcp(binding.inner);
            bindings.bind(inner, bindPort(binding.exposed));
        }
        return bindings;
    }

    private String toEnvString(Map.Entry<String, String> environmentEntry) {
        return environmentEntry.getKey() + "=" + environmentEntry.getValue();
    }

    private void ensureImageExists(String wantedImage) {
        try {
            dockerClient.inspectImageCmd(wantedImage).exec();
        } catch (NotFoundException e) {
            dockerClient.pullImageCmd(wantedImage).exec(new PullImageResultCallback()).awaitSuccess();
        }
    }
}
