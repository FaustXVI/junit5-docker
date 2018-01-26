package com.github.junit5docker;

import java.util.Map;
import java.util.stream.Stream;

interface DockerClientAdapter {

    ContainerInfo startContainer(String wantedImage,
                                 Map<String, String> environment,
                                 String[] networkNames,
                                 PortBinding... portBinding);

    void disconnectFromNetwork(String containerId, String networkId);

    /**
     * Remove the specified network if no containers are connected to it.
     *
     * @param networkId The id of the network to try remove
     */
    void maybeRemoveNetwork(String networkId);

    void stopAndRemoveContainer(String containerId);

    Stream<String> logs(String containerId);
}
