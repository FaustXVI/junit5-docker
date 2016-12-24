package com.github.junit5docker.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.error.BasicErrorMessageFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ExecutorAssertions extends AbstractAssert<ExecutorAssertions, ExecutorService> {

    private ExecutorAssertions(ExecutorService actual) {
        super(actual, ExecutorAssertions.class);
    }

    public static ExecutorAssertions assertThat(ExecutorService actual) {
        return new ExecutorAssertions(actual);
    }

    public ExecutorAssertions isShutedDownBefore(int timeout, TimeUnit timeUnit) throws InterruptedException {
        boolean await = actual.awaitTermination(timeout, timeUnit);
        if (!await) throwAssertionError(new ShouldBeShutedDown(timeout, timeUnit));
        return this;
    }

    private static class ShouldBeShutedDown extends BasicErrorMessageFactory {

        ShouldBeShutedDown(int timeout, TimeUnit timeUnit) {
            super("Executor expected to be shuted down before %d %s", timeout, timeUnit);
        }
    }
}
