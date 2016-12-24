package com.github.junit5docker.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.error.BasicErrorMessageFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

public final class CountDownLatchAssertions extends AbstractAssert<CountDownLatchAssertions, CountDownLatch> {

    private CountDownLatchAssertions(CountDownLatch actual) {
        super(actual, CountDownLatchAssertions.class);
    }

    public static CountDownLatchAssertions assertThat(CountDownLatch actual) {
        return new CountDownLatchAssertions(actual);
    }

    public CountDownLatchAssertions isDownBefore(int timeout, TimeUnit timeUnit) {
        boolean await = waitFor(timeout, timeUnit);
        if (!await) throwAssertionError(new ShouldBeDownBefore(timeout, timeUnit));
        return this;
    }

    public CountDownLatchAssertions isUpAfter(int timeout, TimeUnit timeUnit) {
        boolean await = waitFor(timeout, timeUnit);
        if (await) throwAssertionError(new ShouldBeUpAfter(timeout, timeUnit));
        return this;
    }

    private boolean waitFor(int timeout, TimeUnit timeUnit) {
        boolean await = false;
        try {
            await = actual.await(timeout, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test has been interrupted");
        }
        return await;
    }

    private static class ShouldBeDownBefore extends BasicErrorMessageFactory {

        ShouldBeDownBefore(int timeout, TimeUnit timeUnit) {
            super("Count down latch expected to be down after %d %s", timeout, timeUnit);
        }
    }

    private static class ShouldBeUpAfter extends BasicErrorMessageFactory {

        ShouldBeUpAfter(int timeout, TimeUnit timeUnit) {
            super("Count down latch expected to still be up after %d %s", timeout, timeUnit);
        }
    }
}
