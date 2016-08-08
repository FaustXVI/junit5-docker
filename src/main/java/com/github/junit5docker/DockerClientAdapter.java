package com.github.junit5docker;

interface DockerClientAdapter {
    String startContainer(String wantedImage, PortBinding... portBinding);

    void stopAndRemoveContainer(String containerId);
}
