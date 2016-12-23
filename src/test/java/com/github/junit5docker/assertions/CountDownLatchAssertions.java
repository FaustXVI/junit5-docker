package com.github.junit5docker.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Failures;
import org.assertj.core.util.Strings;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class CountDownLatchAssertions extends AbstractAssert<CountDownLatchAssertions, CountDownLatch> {

    private static final Failures FAILURES = Failures.instance();

    private CountDownLatchAssertions(CountDownLatch actual) {
        super(actual, CountDownLatchAssertions.class);
    }

    public static CountDownLatchAssertions assertThat(CountDownLatch actual) {
        return new CountDownLatchAssertions(actual);
    }

    public CountDownLatchAssertions isDownBefore(int timeout, TimeUnit timeUnit) throws InterruptedException {
        boolean await = actual.await(timeout, timeUnit);
        if (await) return this;
        throw Optional.ofNullable(FAILURES.failureIfErrorMessageIsOverridden(info))
            .orElse(new AssertionError(Strings.formatIfArgs("Count down latch expected to be down after %d %s",
                timeout, timeUnit)));
    }

    public CountDownLatchAssertions isUpAfter(int timeout, TimeUnit timeUnit) throws InterruptedException {
        boolean await = actual.await(timeout, timeUnit);
        if (await) {
            throw Optional.ofNullable(FAILURES.failureIfErrorMessageIsOverridden(info))
                .orElse(new AssertionError(Strings.formatIfArgs("Count down latch expected to still be up after %d %s",
                    timeout, timeUnit)));
        } else {
            return this;
        }
    }
}
