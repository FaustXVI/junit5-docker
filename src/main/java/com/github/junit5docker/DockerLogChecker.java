package com.github.junit5docker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

class DockerLogChecker {

    private final DockerClientAdapter dockerClient;

    DockerLogChecker(DockerClientAdapter dockerClient) {
        this.dockerClient = dockerClient;
    }

    void waitForLogAccordingTo(WaitFor waitFor, String containerId) {
        String expectedLog = waitFor.value();
        if (!WaitFor.NOTHING.equals(expectedLog)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CompletableFuture<Boolean> logFound = supplyAsync(findFirstLogContaining(expectedLog, containerId),
                                                              executor);
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
}
