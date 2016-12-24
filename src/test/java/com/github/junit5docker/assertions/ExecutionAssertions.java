package com.github.junit5docker.assertions;

import org.assertj.core.internal.Failures;

public final class ExecutionAssertions {

    private static final Failures FAILURES = Failures.instance();

    private ExecutionAssertions() {
    }

    public static Runnable assertNoInterruptionThrown(InterruptibleRunnable callable) {
        return () -> {
            try {
                callable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw FAILURES.failure("Test has been interrupted");
            }
        };
    }

    @FunctionalInterface
    public interface InterruptibleRunnable {

        void run() throws InterruptedException;
    }

}
