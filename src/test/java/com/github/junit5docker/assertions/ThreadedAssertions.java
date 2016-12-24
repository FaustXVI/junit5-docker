package com.github.junit5docker.assertions;

import org.assertj.core.api.AbstractAssert;

import java.util.concurrent.ExecutionException;

public final class ThreadedAssertions<T, E extends Exception>
    extends AbstractAssert<ThreadedAssertions<T, E>, ThreadedAssertions.ThrowableSupplier<T, E>> {

    private ThreadedAssertions(ThrowableSupplier<T, E> actual) {
        super(actual, ThreadedAssertions.class);
    }

    public T hasNoAssertionFailures() throws ExecutionException, E {
        try {
            return actual.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            } else {
                throw e;
            }
        }
    }

    public static <T, E extends Exception> ThreadedAssertions<T, E> assertExecutionOf(ThrowableSupplier<T, E> o) {
        return new ThreadedAssertions<>(o);
    }

    @FunctionalInterface
    public interface ThrowableSupplier<T, E extends Exception> {

        T get() throws ExecutionException, E;
    }
}
