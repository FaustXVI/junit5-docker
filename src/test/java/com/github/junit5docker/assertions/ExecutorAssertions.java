package com.github.junit5docker.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Failures;
import org.assertj.core.util.Strings;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ExecutorAssertions extends AbstractAssert<ExecutorAssertions, ExecutorService> {

    private static final Failures FAILURES = Failures.instance();

    private ExecutorAssertions(ExecutorService actual) {
        super(actual, ExecutorAssertions.class);
    }

    public static ExecutorAssertions assertThat(ExecutorService actual) {
        return new ExecutorAssertions(actual);
    }

    public ExecutorAssertions isShutedDownBefore(int timeout, TimeUnit timeUnit) throws InterruptedException {
        boolean await = actual.awaitTermination(timeout, timeUnit);
        if (await) return this;
        throw Optional.ofNullable(FAILURES.failureIfErrorMessageIsOverridden(info))
            .orElse(new AssertionError(Strings.formatIfArgs("Executor expected to be shuted down before %d %s",
                timeout, timeUnit)));
    }

}
