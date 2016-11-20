package com.github.junit5docker;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class OpenQueueIteratorTest {

    @Test
    public void shouldHaveNothingIfClosed() throws InterruptedException {
        ArrayBlockingQueue<String> lines = new ArrayBlockingQueue<>(1);
        lines.put("a line");
        OpenQueueIterator iterator = new OpenQueueIterator(new AtomicBoolean(false), lines);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void shouldHaveContentIfDataIsInQueue() throws InterruptedException {
        ArrayBlockingQueue<String> lines = new ArrayBlockingQueue<>(1);
        lines.put("a line");
        OpenQueueIterator iterator = new OpenQueueIterator(new AtomicBoolean(true), lines);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("a line");
    }

    @Test
    public void shouldInterruptIfBooleanPassesToFalse() throws InterruptedException, TimeoutException, ExecutionException {
        ArrayBlockingQueue<String> lines = new ArrayBlockingQueue<>(1);
        AtomicBoolean opened = new AtomicBoolean(true);
        OpenQueueIterator iterator = new OpenQueueIterator(opened, lines);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        Future<Boolean> hasNext = executor.submit(iterator::hasNext);
        executor.schedule(() -> opened.set(false), 50, MILLISECONDS);
        executor.shutdown();
        assertThat(executor.awaitTermination(200, MILLISECONDS)).isTrue();
        assertThat(hasNext.get()).isFalse();
    }

    @Test
    public void shouldGiveFirstLineEvenAfterTwoCallToHasNext() throws InterruptedException {
        ArrayBlockingQueue<String> lines = new ArrayBlockingQueue<>(1);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.submit(() -> {
            try {
                lines.put("a line");
            } catch (InterruptedException e) {
            }
        });
        executor.schedule(() -> {
            try {
                lines.put("a line 2");
            } catch (InterruptedException e) {
            }
        }, 20, MILLISECONDS);
        OpenQueueIterator iterator = new OpenQueueIterator(new AtomicBoolean(true), lines);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("a line");
        executor.shutdownNow();
    }

    @Test
    public void shouldInterruptIfThreadIsInterrupted() throws InterruptedException, TimeoutException,
            ExecutionException {
        ArrayBlockingQueue<String> lines = new ArrayBlockingQueue<>(1);
        AtomicBoolean opened = new AtomicBoolean(true);
        OpenQueueIterator iterator = new OpenQueueIterator(opened, lines);
        Thread.currentThread().interrupt();
        assertThat(iterator.hasNext()).isFalse();
        assertThat(Thread.interrupted()).isTrue();
        assertThat(opened.get()).isFalse();
    }


    @Test
    public void shouldReadLineOnlyOnce() throws InterruptedException {
        AtomicBoolean opened = new AtomicBoolean(true);
        ArrayBlockingQueue<String> lines = new ArrayBlockingQueue<>(1);
        lines.put("a line");
        OpenQueueIterator iterator = new OpenQueueIterator(opened, lines);
        iterator.hasNext();
        iterator.next();
        opened.set(false);
        assertThat(iterator.hasNext()).isFalse();
    }
}