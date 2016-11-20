package com.github.junit5docker;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.github.junit5docker.ExecutorSanitizer.ignoreInterrupted;
import static com.github.junit5docker.ExecutorSanitizer.verifyAssertionError;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class StreamLogTest {

    private StreamLog streamLog;

    private ExecutorService executor;

    @BeforeEach
    public void createStreamLog() {
        streamLog = new StreamLog();
    }

    @BeforeEach
    public void createExecutor() {
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    public void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    public void shouldGiveAnEmptyStreamWhenClosedImmediatly() throws InterruptedException, ExecutionException {
        streamLog.onComplete();
        CountDownLatch streamCollected = new CountDownLatch(1);
        Future<List<String>> logs = executor.submit(() -> {
            List<String> collectedLogs = streamLog.stream().collect(toList());
            streamCollected.countDown();
            return collectedLogs;
        });
        assertThat(streamCollected.await(10, MILLISECONDS))
                .overridingErrorMessage("Stream should close when onComplete is called")
                .isTrue();
        assertThat(logs.get()).isEmpty();
    }

    @Test
    public void shouldGiveAnEmptyStreamWhenFailsImmediatly() throws InterruptedException, ExecutionException, TimeoutException {
        streamLog.onError(Mockito.mock(Throwable.class));
        CountDownLatch streamCollected = new CountDownLatch(1);
        Future<List<String>> logs = executor.submit(() -> {
            List<String> collectedLogs = streamLog.stream().collect(toList());
            streamCollected.countDown();
            return collectedLogs;
        });
        assertThat(streamCollected.await(10, MILLISECONDS))
                .overridingErrorMessage("Stream should close when onError is called")
                .isTrue();
        assertThat(logs.get()).isEmpty();
    }

    @Test
    public void shouldGiveAStreamContainingLineOfFrameFromOtherThread() throws Throwable {
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch streamStarted = new CountDownLatch(1);
        executor.submit(ignoreInterrupted(() -> {
            streamRequested.await();
            streamLog.onNext(new Frame(StreamType.RAW, "added line".getBytes()));
        }));
        Stream<String> logs = streamLog.stream().peek((l) -> streamStarted.countDown());
        streamRequested.countDown();
        Future<?> streamCompleted = executor.submit(ignoreInterrupted(() -> {
            try {
                assertThat(streamStarted.await(100, MILLISECONDS))
                        .overridingErrorMessage("Stream should have been started")
                        .isTrue();
            } finally {
                streamLog.onComplete();
            }
        }));
        Future<?> haveLogs = executor.submit(() -> assertThat(logs).contains("added line"));
        verifyAssertionError(streamCompleted::get);
        try {
            verifyAssertionError(() -> haveLogs.get(100, MILLISECONDS));
        } catch (TimeoutException e) {
            fail("Stream should have closed");
        }
    }

    @Test
    public void shouldInterruptStreamWhenDockerThreadInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        streamLog.onNext(new Frame(StreamType.RAW, "added line".getBytes()));
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    public void shouldNotCloseStreamIfDockerIsStillRunning() throws InterruptedException {
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch executionFinished = new CountDownLatch(1);
        executor.submit(ignoreInterrupted(() -> {
            streamRequested.await();
            streamLog.stream().collect(toList());
            executionFinished.countDown();
        }));
        streamRequested.countDown();
        assertThat(executionFinished.await(100, MILLISECONDS))
                .overridingErrorMessage("Stream should not have finished")
                .isFalse();
    }

    @Test
    public void shouldInterruptStreamWhenReadingThreadInterrupted() throws Throwable {
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch executionStarted = new CountDownLatch(1);
        Future<?> threadStillInterrupted = executor.submit(ignoreInterrupted(() -> {
            streamRequested.await();
            executionStarted.countDown();
            streamLog.stream().collect(toList());
            assertThat(currentThread().isInterrupted())
                    .overridingErrorMessage("Thread should keep its interruption state")
                    .isTrue();
        }));
        streamRequested.countDown();
        executionStarted.await();
        executor.shutdownNow();
        assertThat(executor.awaitTermination(100, MILLISECONDS))
                .overridingErrorMessage("Stream should have ended")
                .isTrue();
        verifyAssertionError(threadStillInterrupted::get);
    }
}