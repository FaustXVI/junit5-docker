package com.github.junit5docker;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class StreamLogTest {

    private StreamLog streamLog;

    @BeforeEach
    public void createStreamLog() {
        streamLog = new StreamLog();
    }

    @Test
    public void shouldGiveAnEmptyStreamWhenClosedImmediatly() throws InterruptedException, ExecutionException {
        streamLog.onComplete();
        CountDownLatch streamCollected = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> logs = executor.submit(() -> {
            List<String> collectedLogs = streamLog.stream().collect(toList());
            streamCollected.countDown();
            return collectedLogs;
        });
        assertThat(streamCollected.await(10, MILLISECONDS))
                .overridingErrorMessage("Stream should close when onComplete is called")
                .isTrue();
        assertThat(logs.get()).isEmpty();
        executor.shutdownNow();
    }

    @Test
    public void shouldGiveAnEmptyStreamWhenFailsImmediatly() throws InterruptedException, ExecutionException, TimeoutException {
        streamLog.onError(Mockito.mock(Throwable.class));
        CountDownLatch streamCollected = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> logs = executor.submit(() -> {
            List<String> collectedLogs = streamLog.stream().collect(toList());
            streamCollected.countDown();
            return collectedLogs;
        });
        assertThat(streamCollected.await(10, MILLISECONDS))
                .overridingErrorMessage("Stream should close when onError is called")
                .isTrue();
        assertThat(logs.get()).isEmpty();
        executor.shutdownNow();
    }

    @Test
    public void shouldGiveAStreamContainingLineOfFrameFromOtherThread() throws Throwable {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch streamStarted = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                streamRequested.await();
                streamLog.onNext(new Frame(StreamType.RAW, "added line".getBytes()));
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        });
        Stream<String> logs = streamLog.stream().peek((l) -> streamStarted.countDown());
        streamRequested.countDown();
        Future<?> completed = executor.submit(() -> {
            try {
                assertThat(streamStarted.await(100, MILLISECONDS))
                        .overridingErrorMessage("Stream should have been started")
                        .isTrue();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                streamLog.onComplete();
            }
        });
        Future<?> haveLogs = executor.submit(() -> assertThat(logs).contains("added line"));
        try {
            completed.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
        try {
            haveLogs.get(100, MILLISECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        } catch (TimeoutException e) {
            fail("Stream should have closed");
        }
        executor.shutdownNow();
    }

    @Test
    public void shouldInterruptStreamWhenDockerThreadInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        streamLog.onNext(new Frame(StreamType.RAW, "added line".getBytes()));
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    public void shouldNotCloseStreamIfDockerIsStillRunning() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch executionFinished = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                streamRequested.await();
                streamLog.stream().collect(toList());
                executionFinished.countDown();
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        });
        streamRequested.countDown();
        assertThat(executionFinished.await(100, MILLISECONDS))
                .overridingErrorMessage("Stream should not have finished")
                .isFalse();
        executor.shutdownNow();
    }

    @Test
    public void shouldInterruptStreamWhenReadingThreadInterrupted() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch executionStarted = new CountDownLatch(1);
        AtomicReference<Boolean> stillInterrupted = new AtomicReference<>();
        executor.submit(() -> {
            try {
                streamRequested.await();
                executionStarted.countDown();
                streamLog.stream().collect(toList());
                stillInterrupted.set(currentThread().isInterrupted());
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        });
        streamRequested.countDown();
        executionStarted.await();
        executor.shutdownNow();
        assertThat(executor.awaitTermination(100, MILLISECONDS))
                .overridingErrorMessage("Stream should have ended")
                .isTrue();
        assertThat(stillInterrupted.get())
                .overridingErrorMessage("Thread should keep its interruption state")
                .isTrue();
    }
}