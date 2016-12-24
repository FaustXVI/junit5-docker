package com.github.junit5docker.assertions;

import static org.junit.jupiter.api.Assertions.fail;

public final class ExecutionAssertions {

    private ExecutionAssertions() {
    }

    public static Runnable assertNoInterruptionThrown(InterruptibleRunnable callable) {
        return () -> {
            try {
                callable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test has been interrupted");
            }
        };
    }

    @FunctionalInterface
    public interface InterruptibleRunnable {

        void run() throws InterruptedException;
    }

}
