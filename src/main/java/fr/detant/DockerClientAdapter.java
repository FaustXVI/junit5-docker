package fr.detant;

interface DockerClientAdapter {
    String startContainer(String wantedImage, PortBinding... portBinding);

    void stopAndRemoveContainer(String containerId);
}
