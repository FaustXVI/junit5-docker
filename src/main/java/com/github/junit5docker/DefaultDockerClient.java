package com.github.junit5docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static com.github.dockerjava.core.DockerClientConfig.createDefaultConfigBuilder;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;

class DefaultDockerClient implements DockerClientAdapter {

    private final DockerClient dockerClient;

    DefaultDockerClient() {
        dockerClient = DockerClientBuilder.getInstance(createDefaultConfigBuilder().withApiVersion("1.22")).build();
    }

    @Override
    public String startContainer(String wantedImage, Map<String, String> environment, PortBinding... portBinding) {
        Ports bindings = createPortBindings(portBinding);
        List<String> environmentStrings = createEnvironmentList(environment);
        String containerId = createContainer(wantedImage, bindings, environmentStrings);
        dockerClient.startContainerCmd(containerId).exec();
        return containerId;
    }

    @Override
    public void stopAndRemoveContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).exec();
    }

    @Override
    public Stream<String> logs(String containerID) {
        return dockerClient.logContainerCmd(containerID).withFollowStream(true)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new StreamLog())
                .stream();
    }

    private String createContainer(String wantedImage, Ports bindings, List<String> environmentStrings) {
        if (!wantedImage.contains(":")) {
            wantedImage += ":latest";
        }
        this.ensureImageExists(wantedImage);
        return dockerClient.createContainerCmd(wantedImage)
                .withEnv(environmentStrings)
                .withPortBindings(bindings)
                .exec().getId();
    }

    private List<String> createEnvironmentList(Map<String, String> environment) {
        return environment.entrySet().stream().map(this::toEnvString).collect(toList());
    }

    private Ports createPortBindings(PortBinding[] portBinding) {
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

    private static class StreamLog extends LogContainerResultCallback {

        private volatile String line;

        private volatile boolean opened = true;

        @Override
        public void onNext(Frame item) {
            line = new String(item.getPayload());
        }

        @Override
        public void onComplete() {
            opened = false;
        }

        public Stream<String> stream() {
            return StreamSupport.stream(spliteratorUnknownSize(new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    while (opened && line == null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                    return opened;
                }

                @Override
                public String next() {
                    String currentLine = line;
                    line = null;
                    return currentLine;
                }
            }, 0), false);
        }
    }
}
