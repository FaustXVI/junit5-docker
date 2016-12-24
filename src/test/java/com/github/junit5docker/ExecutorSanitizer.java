package com.github.junit5docker;

import org.assertj.core.internal.Failures;

import java.util.function.Supplier;

final class ExecutorSanitizer {

    private static final Failures FAILURES = Failures.instance();

    private ExecutorSanitizer() {
    }

    static Runnable ignoreInterrupted(InterruptibleRunnable callable) {
        return () -> {
            try {
                callable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw FAILURES.failure("Test has been interrupted");
            }
        };
    }

    static <T> Supplier<T> ignoreInterrupted(InterruptibleSupplier<T> callable) {
        return () -> {
            try {
                return callable.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw FAILURES.failure("Test has been interrupted");
            }
        };
    }

    @FunctionalInterface
    interface InterruptibleRunnable {

        void run() throws InterruptedException;
    }

    @FunctionalInterface
    interface InterruptibleSupplier<T> {

        T get() throws InterruptedException;
    }

}
