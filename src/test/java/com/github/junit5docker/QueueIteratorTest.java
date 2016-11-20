package com.github.junit5docker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static com.github.junit5docker.InterruptibleRunnable.ignoreInterrupted;
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
    public void shouldReadLineOnlyOnce() throws InterruptedException {
        lines.put("a line");
        iterator.hasNext();
        iterator.next();
        iterator.close();
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void shouldInterruptIfThreadIsInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        assertThat(iterator.hasNext()).isFalse();
        assertThat(Thread.interrupted()).isTrue();
    }

    @Nested
    class InAMultiThreadContext {

        private ExecutorService executor;

        @BeforeEach
        public void createOtherThread() {
            executor = Executors.newSingleThreadExecutor();
        }

        @AfterEach
        public void stopExecutor() {
            executor.shutdownNow();
        }

        @Test
        void shouldInterruptIfClosedByAnotherThread() throws InterruptedException {
            CountDownLatch hasNextStarted = new CountDownLatch(1);
            CountDownLatch hasNextReturned = new CountDownLatch(1);
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
        }

        @Test
        void shouldGiveFirstLineEvenAfterTwoCallToHasNext() throws InterruptedException {
            CountDownLatch firstLinePushed = new CountDownLatch(1);
            executor.submit(ignoreInterrupted(() -> {
                lines.put("a line");
                firstLinePushed.countDown();
            }));
            executor.submit(ignoreInterrupted(() -> {
                firstLinePushed.await();
                lines.put("a line 2");
            }));
            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.next()).isEqualTo("a line");
        }
    }
}