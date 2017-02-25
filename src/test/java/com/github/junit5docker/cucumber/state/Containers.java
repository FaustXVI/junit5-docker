package com.github.junit5docker.cucumber.state;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.dockerjava.core.DockerClientConfig.createDefaultConfigBuilder;
import static org.assertj.core.api.Assertions.assertThat;

public class Containers {

    private final DockerClient dockerClient = DockerClientBuilder
        .getInstance(createDefaultConfigBuilder().withApiVersion("1.22"))
        .build();

    private List<Container> existingContainers;

    private List<Container> containersStartedByExtension;

    private List<Container> remainingContainers;

    private List<Container> containersStartedByExtensionPerTest;

    private List<Container> remainingContainersPerTest;

    private List<InspectContainerResponse> containersInspect;

    private List<LogCallback> logs;

    public Containers() {
        existingContainers = dockerClient.listContainersCmd().exec();
    }

    private List<Container> getContainers() {
        return dockerClient.listContainersCmd().exec().stream()
            .filter(container -> !existingContainers.contains(container))
            .collect(Collectors.toList());
    }

    public void updateStarted() {
        System.out.println("Containers.updateStarted");
        containersStartedByExtension = getContainers();
        containersInspect = containersStartedByExtension.stream()
            .map(c -> dockerClient.inspectContainerCmd(c.getId()).exec())
            .collect(Collectors.toList());
        logs = containersStartedByExtension.stream()
            .map(c -> dockerClient.logContainerCmd(c.getId())
                .withStdOut(true)
                .withStdErr(true)
                .exec(new LogCallback()))
            .collect(Collectors.toList());
    }

    public void updateStartedForTest() {
        containersStartedByExtensionPerTest = getContainers().stream()
            .filter(c -> !containersStartedByExtension.contains(c))
            .collect(Collectors.toList());
    }

    public void updateRemainings() {
        remainingContainers = getContainers();
    }

    public void updateRemainingsForTest() {
        remainingContainersPerTest = getContainers();
    }

    public void verifyAllClean() {
        remainingContainers
            .forEach(container -> {
                dockerClient.stopContainerCmd(container.getId()).exec();
                dockerClient.removeContainerCmd(container.getId()).exec();
            });
        assertThat(remainingContainers).isEmpty();
    }

    public Stream<Integer[]> portMapping() {
        return containersStartedByExtension.stream()
            .map(Container::getPorts)
            .flatMap(Stream::of)
            .map(port -> new Integer[]{port.getPublicPort(), port.getPrivatePort()});
    }

    public Stream<String> startedImageNames() {
        return containersStartedByExtension.stream().map(Container::getImage);
    }

    public Stream<String> startedImageNamesPerTest() {
        return containersStartedByExtensionPerTest.stream().map(Container::getImage);
    }

    public Stream<String> environment() {
        return containersInspect.stream().map((c) -> c.getConfig().getEnv()).flatMap(Stream::of);
    }

    public Stream<String> logs() {
        return logs.stream().map(LogCallback::toString);
    }

    public List<Container> remainings() {
        return remainingContainers;
    }

    public Stream<Container> startedContainersPerTest() {
        return containersStartedByExtensionPerTest.stream();
    }

    private static class LogCallback extends LogContainerResultCallback {

        private StringBuilder logs = new StringBuilder();

        @Override
        public void onNext(Frame item) {
            logs.append(new String(item.getPayload()));
        }

        @Override
        public String toString() {
            return logs.toString();
        }
    }
}
