package com.github.junit5docker;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.currentThread;
import static org.assertj.core.api.Assertions.assertThat;

public class StreamLogTest {

    private StreamLog streamLog;

    @BeforeEach
    public void createStreamLog() {
        streamLog = new StreamLog();
    }

    @Test
    public void shouldGiveAnEmptyStreamWhenClosedImmediatly() throws InterruptedException, ExecutionException, TimeoutException {
        streamLog.onComplete();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> logs = executor.submit(() -> streamLog.stream().collect(Collectors.toList()));
        assertThat(logs.get(1, TimeUnit.MILLISECONDS)).isEmpty();
    }

    @Test
    public void shouldGiveAnEmptyStreamWhenFailsImmediatly() throws InterruptedException, ExecutionException, TimeoutException {
        streamLog.onError(Mockito.mock(Throwable.class));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> logs = executor.submit(() -> streamLog.stream().collect(Collectors.toList()));
        assertThat(logs.get(10, TimeUnit.MILLISECONDS)).isEmpty();
    }

    @Test
    public void shouldGiveAStreamContainingLineOfFrameFromOtherThread() throws InterruptedException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch logAdded = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                streamRequested.await();
                streamLog.onNext(new Frame(StreamType.RAW, "added line".getBytes()));
                logAdded.countDown();
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        });
        Stream<String> logs = streamLog.stream();
        streamRequested.countDown();
        logAdded.await();
        executor.schedule(streamLog::onComplete, 100, TimeUnit.MILLISECONDS);
        assertThat(logs).contains("added line");
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
        CountDownLatch executionStarted = new CountDownLatch(1);
        CountDownLatch executionFinished = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                streamRequested.await();
                executionStarted.countDown();
                streamLog.stream().collect(Collectors.toList());
                executionFinished.countDown();
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        });
        streamRequested.countDown();
        executionStarted.await();
        assertThat(executionFinished.await(100, TimeUnit.MILLISECONDS)).isFalse();
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
                streamLog.stream().collect(Collectors.toList());
                stillInterrupted.set(currentThread().isInterrupted());
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        });
        streamRequested.countDown();
        executionStarted.await();
        executor.shutdownNow();
        assertThat(executor.awaitTermination(100, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(stillInterrupted.get()).isTrue();
    }
}