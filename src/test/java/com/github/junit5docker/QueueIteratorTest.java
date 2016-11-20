package com.github.junit5docker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class QueueIteratorTest {

    private BlockingQueue<String> lines;

    private QueueIterator iterator;

    @BeforeEach
    public void createIterater() {
        lines = new ArrayBlockingQueue<>(1);
        iterator = new QueueIterator(lines);
    }

    @Test
    public void shouldHaveNothingIfClosed() throws InterruptedException {
        iterator.close();
        lines.put("a line");
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void shouldHaveContentIfDataIsInQueue() throws InterruptedException {
        lines.put("a line");
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("a line");
    }

    @Test
    public void shouldInterruptIfClosedByAnotherThread() throws InterruptedException {
        CountDownLatch hasNextStarted = new CountDownLatch(1);
        CountDownLatch hasNextReturned = new CountDownLatch(1);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.submit(() -> {
            hasNextStarted.countDown();
            iterator.hasNext();
            hasNextReturned.countDown();
        });
        hasNextStarted.await();
        iterator.close();
        assertThat(hasNextReturned.await(100, MILLISECONDS))
                .overridingErrorMessage("hasNext should have returned")
                .isTrue();
        executor.shutdown();
    }

    @Test
    public void shouldGiveFirstLineEvenAfterTwoCallToHasNext() throws InterruptedException {
        CountDownLatch firstLinePushed = new CountDownLatch(1);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.submit(() -> {
            try {
                lines.put("a line");
                firstLinePushed.countDown();
            } catch (InterruptedException e) {
            }
        });
        executor.submit(() -> {
            try {
                firstLinePushed.await();
                lines.put("a line 2");
            } catch (InterruptedException e) {
            }
        });
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("a line");
        executor.shutdownNow();
    }

    @Test
    public void shouldInterruptIfThreadIsInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        assertThat(iterator.hasNext()).isFalse();
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    public void shouldReadLineOnlyOnce() throws InterruptedException {
        lines.put("a line");
        iterator.hasNext();
        iterator.next();
        iterator.close();
        assertThat(iterator.hasNext()).isFalse();
    }
}