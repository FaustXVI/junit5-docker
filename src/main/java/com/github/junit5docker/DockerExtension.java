package com.github.junit5docker;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

class DockerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private final DockerClientAdapter dockerClient;

    private String containerId;

    DockerExtension() {
        this(new DefaultDockerClient());
    }

    DockerExtension(DockerClientAdapter dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void beforeAll(ExtensionContext containerExtensionContext) {
        Docker dockerAnnotation = findDockerAnnotation(containerExtensionContext);
        if (!dockerAnnotation.newForEachCase()) startContainer(dockerAnnotation);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Docker dockerAnnotation = findDockerAnnotation(context);
        if (dockerAnnotation.newForEachCase()) startContainer(dockerAnnotation);
    }

    private void startContainer(Docker dockerAnnotation) {
        PortBinding[] portBindings = createPortBindings(dockerAnnotation);
        Map<String, String> environmentMap = createEnvironmentMap(dockerAnnotation);
        String imageReference = findImageName(dockerAnnotation);
        WaitFor waitFor = dockerAnnotation.waitFor();
        containerId = dockerClient.startContainer(imageReference, environmentMap, portBindings);
        waitForLogAccordingTo(waitFor);
    }

    private void waitForLogAccordingTo(WaitFor waitFor) {
        String expectedLog = waitFor.value();
        if (!WaitFor.NOTHING.equals(expectedLog)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CompletableFuture<Boolean> logFound = supplyAsync(findFirstLogContaining(expectedLog), executor);
            executor.shutdown();
            try {
                boolean termination = executor.awaitTermination(waitFor.timeoutInMillis(), TimeUnit.MILLISECONDS);
                if (!termination) {
                    throw new AssertionError("Timeout while waiting for log : \"" + expectedLog + "\"");
                }
                if (!logFound.getNow(false)) {
                    throw new AssertionError("\"" + expectedLog + "\" not found in logs and container stopped");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Supplier<Boolean> findFirstLogContaining(String logToFind) {
        return () -> {
            try (Stream<String> logs = dockerClient.logs(containerId)) {
                return logs.anyMatch(log -> log.contains(logToFind));
            }
        };
    }

    private Docker findDockerAnnotation(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getTestClass().get();
        return testClass.getAnnotation(Docker.class);
    }

    private String findImageName(Docker dockerAnnotation) {
        return dockerAnnotation.image();
    }

    private Map<String, String> createEnvironmentMap(Docker dockerAnnotation) {
        Map<String, String> environmentMap = new HashMap<>();
        Environment[] environments = dockerAnnotation.environments();
        for (Environment environment : environments) {
            environmentMap.put(environment.key(), environment.value());
        }
        return environmentMap;
    }

    private PortBinding[] createPortBindings(Docker dockerAnnotation) {
        Port[] ports = dockerAnnotation.ports();
        PortBinding[] portBindings = new PortBinding[ports.length];
        for (int i = 0; i < ports.length; i++) {
            Port port = ports[i];
            portBindings[i] = new PortBinding(port.exposed(), port.inner());
        }
        return portBindings;
    }

    @Override
    public void afterAll(ExtensionContext containerExtensionContext) {
        Docker dockerAnnotation = findDockerAnnotation(containerExtensionContext);
        if (!dockerAnnotation.newForEachCase()) dockerClient.stopAndRemoveContainer(containerId);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Docker dockerAnnotation = findDockerAnnotation(context);
        if (dockerAnnotation.newForEachCase()) dockerClient.stopAndRemoveContainer(containerId);
    }
}
