package com.github.junit5docker;

import java.util.Map;
import java.util.stream.Stream;

interface DockerClientAdapter {
    String startContainer(String wantedImage, Map<String, String> environment, PortBinding... portBinding);

    void stopAndRemoveContainer(String containerId);

    Stream<String> logs(String containerId);
}
