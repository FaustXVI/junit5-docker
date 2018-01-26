package com.github.junit5docker;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

class DockerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private final DockerClientAdapter dockerClient;

    private final DockerLogChecker dockerLogChecker;

    DockerExtension() {
        this(new DefaultDockerClient());
    }

    DockerExtension(DockerClientAdapter dockerClient) {
        this.dockerClient = dockerClient;
        this.dockerLogChecker = new DockerLogChecker(dockerClient);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        forEachDocker(context, docker -> !docker.newForEachCase(), this::startContainer);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        forEachDocker(context, Docker::newForEachCase, this::startContainer);
    }

    private static void forEachDocker(ExtensionContext context,
                                      Predicate<Docker> predicate,
                                      BiConsumer<ExtensionContext, Docker> action) {
        Arrays.stream(findDockerAnnotations(context))
              .filter(predicate)
              .forEach(docker -> action.accept(context, docker));
    }

    private void startContainer(ExtensionContext context, Docker dockerAnnotation) {
        PortBinding[] portBindings = createPortBindings(dockerAnnotation);
        Map<String, String> environmentMap = createEnvironmentMap(dockerAnnotation);
        String imageReference = findImageName(dockerAnnotation);
        WaitFor waitFor = dockerAnnotation.waitFor();
        String[] networkNames = dockerAnnotation.networks();
        ContainerInfo containerInfo = dockerClient.startContainer(imageReference,
                                                                  environmentMap,
                                                                  networkNames,
                                                                  portBindings);
        dockerLogChecker.waitForLogAccordingTo(waitFor, containerInfo.getContainerId());
        getStore(context).put(dockerAnnotation, containerInfo);
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
        forEachDocker(context, docker -> !docker.newForEachCase(), this::stopAndRemove);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        forEachDocker(context, Docker::newForEachCase, this::stopAndRemove);
    }

    private void stopAndRemove(ExtensionContext context, Docker docker) {
        ContainerInfo containerInfo = getStore(context).remove(docker, ContainerInfo.class);

        String containerId = containerInfo.getContainerId();

        containerInfo.getNetworkIds().forEach(networkId -> {
            dockerClient.disconnectFromNetwork(containerId, networkId);
            dockerClient.maybeRemoveNetwork(networkId);
        });
        dockerClient.stopAndRemoveContainer(containerId);
    }

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL);
    }

}
