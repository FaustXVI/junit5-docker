package com.github.junit5docker;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.fail;

final class ExecutorSanitizer {

    private ExecutorSanitizer() {
    }

    static Runnable ignoreInterrupted(InterruptibleRunnable callable) {
        return () -> {
            try {
                callable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test has been interrupted");
                throw new AssertionError("Bug in interruption ignorer");
            }
        };
    }

    static <T> Supplier<T> ignoreInterrupted(InterruptibleSupplier<T> callable) {
        return () -> {
            try {
                return callable.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test has been interrupted");
                throw new AssertionError("Bug in interruption ignorer");
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
