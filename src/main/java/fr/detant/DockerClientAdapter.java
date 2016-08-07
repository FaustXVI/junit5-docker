package fr.detant;

public interface DockerClientAdapter {
    String startContainer(String wantedImage, PortBinding... portBinding);

    void stopAndRemoveContainer(String containerId);
}
