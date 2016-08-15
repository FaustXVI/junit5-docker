package com.github.junit5docker;

import java.util.Map;

interface DockerClientAdapter {
    String startContainer(String wantedImage, Map<String, String> environment, PortBinding... portBinding);

    void stopAndRemoveContainer(String containerId);
}
