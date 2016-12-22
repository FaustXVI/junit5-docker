package com.github.junit5docker;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.fail;


final class ExecutorSanitizer {

    private ExecutorSanitizer() {
    }

    @FunctionalInterface
    interface InterruptibleRunnable {

        void run() throws InterruptedException;
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

    @FunctionalInterface
    interface InterruptibleSupplier<T> {

        T get() throws InterruptedException;
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
    interface ThrowableSupplier<T> {

        T get() throws Exception;
    }

    static <T> T verifyAssertionError(ThrowableSupplier<T> o) throws Throwable {
        try {
            return o.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AssertionError) throw e.getCause();
            else throw e;
        }
    }
}
