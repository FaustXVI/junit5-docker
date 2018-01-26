package com.github.junit5docker;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

class DockerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private final DockerClientAdapter dockerClient;


    DockerExtension() {
        this(new DefaultDockerClient());
    }

    DockerExtension(DockerClientAdapter dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        forEachDocker(context, d -> !d.newForEachCase(), this::startContainer);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        forEachDocker(context, Docker::newForEachCase, this::startContainer);
    }

    private static void forEachDocker(ExtensionContext context, Predicate<Docker> predicate, BiConsumer<ExtensionContext, Docker> action){
        Arrays.stream(findDockerAnnotations(context)).filter(predicate).forEach(d -> action.accept(context, d));
    }

    private void startContainer(ExtensionContext context, Docker dockerAnnotation) {
        PortBinding[] portBindings = createPortBindings(dockerAnnotation);
        Map<String, String> environmentMap = createEnvironmentMap(dockerAnnotation);
        String imageReference = findImageName(dockerAnnotation);
        WaitFor waitFor = dockerAnnotation.waitFor();
        String[] networkNames = dockerAnnotation.networks();
        ContainerInfo containerInfo = dockerClient.startContainer(imageReference, environmentMap, networkNames, portBindings);
        waitForLogAccordingTo(waitFor, containerInfo.getContainerId());
        getStore(context).put(dockerAnnotation, containerInfo);
    }

    private void waitForLogAccordingTo(WaitFor waitFor, String containerId) {
        String expectedLog = waitFor.value();
        if (!WaitFor.NOTHING.equals(expectedLog)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CompletableFuture<Boolean> logFound = supplyAsync(findFirstLogContaining(expectedLog, containerId), executor);
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

    private Supplier<Boolean> findFirstLogContaining(String logToFind, String containerId) {
        return () -> {
            try (Stream<String> logs = dockerClient.logs(containerId)) {
                return logs.anyMatch(log -> log.contains(logToFind));
            }
        };
    }

    private static Docker[] findDockerAnnotations(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getTestClass().get();
        return testClass.getAnnotationsByType(Docker.class);
    }

    private static String findImageName(Docker dockerAnnotation) {
        return dockerAnnotation.image();
    }

    private static Map<String, String> createEnvironmentMap(Docker dockerAnnotation) {
        Map<String, String> environmentMap = new HashMap<>();
        Environment[] environments = dockerAnnotation.environments();
        for (Environment environment : environments) {
            environmentMap.put(environment.key(), environment.value());
        }
        return environmentMap;
    }

    private static PortBinding[] createPortBindings(Docker dockerAnnotation) {
        Port[] ports = dockerAnnotation.ports();
        PortBinding[] portBindings = new PortBinding[ports.length];
        for (int i = 0; i < ports.length; i++) {
            Port port = ports[i];
            portBindings[i] = new PortBinding(port.exposed(), port.inner());
        }
        return portBindings;
    }

    @Override
    public void afterAll(ExtensionContext context) {
        forEachDocker(context, d -> !d.newForEachCase(), this::stopAndRemove);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        forEachDocker(context, Docker::newForEachCase, this::stopAndRemove);
    }

    private void stopAndRemove(ExtensionContext context, Docker docker) {
        ContainerInfo containerInfo = getStore(context).remove(docker, ContainerInfo.class);

        String containerId = containerInfo.getContainerId();

        containerInfo.getNetworkIds().forEach(c -> {
            dockerClient.disconnectFromNetwork(containerId, c);
            dockerClient.maybeRemoveNetwork(c);
        });
        dockerClient.stopAndRemoveContainer(containerId);
    }

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL);
    }

}
