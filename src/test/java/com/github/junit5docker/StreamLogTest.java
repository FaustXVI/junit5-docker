package com.github.junit5docker;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.junit5docker.ExecutorSanitizer.ignoreInterrupted;
import static com.github.junit5docker.assertions.ThreadedAssertions.assertExecutionOf;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

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
    public void shouldGiveAnEmptyStreamWhenFailsImmediatly() throws InterruptedException, ExecutionException {
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
    public void shouldGiveAStreamContainingLineOfFrameFromOtherThread()
        throws ExecutionException, InterruptedException {
        CountDownLatch streamStarted = new CountDownLatch(1);
        executor.submit(ignoreInterrupted(() -> streamLog.onNext(new Frame(StreamType.RAW, "added line".getBytes()))));
        Future<?> streamCompleted = executor.submit(ignoreInterrupted(completeStreamOnceStarted(streamStarted)));
        assertThat(streamLog.stream().peek((l) -> streamStarted.countDown())).contains("added line");
        assertExecutionOf(streamCompleted::get).hasNoAssertionFailures();
    }

    private ExecutorSanitizer.InterruptibleRunnable completeStreamOnceStarted(CountDownLatch streamStarted) {
        return () -> {
            try {
                assertThat(streamStarted.await(100, MILLISECONDS))
                    .overridingErrorMessage("Stream should have been started")
                    .isTrue();
            } finally {
                streamLog.onComplete();
            }
        };
    }

    @Test
    public void shouldInterruptStreamWhenDockerThreadInterrupted() {
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
    public void shouldInterruptStreamWhenReadingThreadInterrupted() throws InterruptedException, ExecutionException {
        CountDownLatch executionStarted = new CountDownLatch(1);
        Future<?> threadStillInterrupted = executor.submit(ignoreInterrupted(() -> {
            executionStarted.countDown();
            streamLog.stream().collect(toList());
            assertThat(currentThread().isInterrupted())
                .overridingErrorMessage("Thread should keep its interruption state")
                .isTrue();
        }));
        executionStarted.await();
        interruptStream();
        assertExecutionOf(threadStillInterrupted::get).hasNoAssertionFailures();
    }

    private void interruptStream() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(100, MILLISECONDS))
            .overridingErrorMessage("Stream should have ended")
            .isTrue();
    }
}
