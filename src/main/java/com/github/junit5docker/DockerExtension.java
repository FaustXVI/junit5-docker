package com.github.junit5docker;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ContainerExtensionContext;

import java.util.HashMap;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * This is the entry point for JUnit5 extension system.
 *
 * @since 1.0
 */
public class DockerExtension implements BeforeAllCallback, AfterAllCallback {

    private final DockerClientAdapter dockerClient;

    private String containerID;

    /**
     * Default constructor used by JUnit5 extension system
     *
     * @since 1.0
     */
    public DockerExtension() {
        this(new DefaultDockerClient());
    }

    DockerExtension(DockerClientAdapter dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void beforeAll(ContainerExtensionContext containerExtensionContext) {
        Docker dockerAnnotation = findDockerAnnotation(containerExtensionContext);
        PortBinding[] portBindings = createPortBindings(dockerAnnotation);
        HashMap<String, String> environmentMap = createEnvironmentMap(dockerAnnotation);
        String imageReference = findImageName(dockerAnnotation);
        WaitFor waitFor = dockerAnnotation.waitFor();
        containerID = dockerClient.startContainer(imageReference, environmentMap, portBindings);
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
        return () -> dockerClient.logs(containerID).anyMatch(log -> log.contains(logToFind));
    }

    private Docker findDockerAnnotation(ContainerExtensionContext containerExtensionContext) {
        Class<?> testClass = containerExtensionContext.getTestClass().get();
        return testClass.getAnnotation(Docker.class);
    }

    private String findImageName(Docker dockerAnnotation) {
        return dockerAnnotation.image();
    }

    private HashMap<String, String> createEnvironmentMap(Docker dockerAnnotation) {
        HashMap<String, String> environmentMap = new HashMap<>();
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
    public void afterAll(ContainerExtensionContext containerExtensionContext) {
        dockerClient.stopAndRemoveContainer(containerID);
    }
}
