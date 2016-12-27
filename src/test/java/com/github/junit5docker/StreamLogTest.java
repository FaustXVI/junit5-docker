package com.github.junit5docker;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.junit5docker.assertions.CountDownLatchAssertions.assertThat;
import static com.github.junit5docker.assertions.ExecutionAssertions.assertNoInterruptionThrown;
import static com.github.junit5docker.assertions.ExecutorAssertions.assertThat;
import static com.github.junit5docker.assertions.ThreadedAssertions.assertExecutionOf;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class StreamLogTest {

    private static final Charset UTF_8 = Charset.forName("UTF8");

    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

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
        assertThat(streamCollected)
            .overridingErrorMessage("Stream should close when onComplete is called")
            .isDownBefore(10, MILLISECONDS);
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
        assertThat(streamCollected)
            .overridingErrorMessage("Stream should close when onError is called")
            .isDownBefore(10, MILLISECONDS);
        assertThat(logs.get()).isEmpty();
    }

    @Test
    public void shouldGiveAStreamContainingLineOfFrameFromOtherThread()
        throws ExecutionException, InterruptedException {
        CountDownLatch streamStarted = new CountDownLatch(1);
        executor.submit(() -> streamLog.onNext(new Frame(StreamType.RAW, "added line".getBytes(UTF_8))));
        Future<?> streamCompleted = executor.submit(completeStreamOnceStarted(streamStarted));
        assertThat(streamLog.stream().peek((l) -> streamStarted.countDown())).contains("added line");
        assertExecutionOf(streamCompleted::get).hasNoAssertionFailures();
    }

    @Test
    public void shouldReadLineAsUTF8()
        throws ExecutionException, InterruptedException {
        CountDownLatch streamFinished = new CountDownLatch(2);
        String originalString = "use of accents é";
        byte[] utf8String = originalString.getBytes(UTF_8);
        byte[] isoString = originalString.getBytes(ISO_8859_1);
        String misinterpretedString = new String(isoString, UTF_8);
        executor.submit(() -> streamLog.onNext(new Frame(StreamType.RAW, utf8String)));
        executor.submit(() -> streamLog.onNext(new Frame(StreamType.RAW, isoString)));
        Future<?> streamCompleted = executor.submit(completeStreamOnceStarted(streamFinished));
        assertThat(streamLog.stream().peek((l) -> streamFinished.countDown()))
            .hasSize(2)
            .containsOnlyOnce(originalString, misinterpretedString);
        assertExecutionOf(streamCompleted::get).hasNoAssertionFailures();
    }

    @Test
    public void shouldInterruptStreamWhenDockerThreadInterrupted() {
        Thread.currentThread().interrupt();
        streamLog.onNext(new Frame(StreamType.RAW, "added line".getBytes(UTF_8)));
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    public void shouldNotCloseStreamIfDockerIsStillRunning() throws InterruptedException {
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch executionFinished = new CountDownLatch(1);
        executor.submit(assertNoInterruptionThrown(() -> {
            streamRequested.await();
            streamLog.stream().collect(toList());
            executionFinished.countDown();
        }));
        streamRequested.countDown();
        assertThat(executionFinished)
            .overridingErrorMessage("Stream should not have finished")
            .isUpAfter(100, MILLISECONDS);
    }

    @Test
    public void shouldInterruptStreamWhenReadingThreadInterrupted() throws InterruptedException, ExecutionException {
        CountDownLatch executionStarted = new CountDownLatch(1);
        Future<?> threadStillInterrupted = executor.submit(() -> {
            executionStarted.countDown();
            streamLog.stream().collect(toList());
            assertThat(currentThread().isInterrupted())
                .overridingErrorMessage("Thread should keep its interruption state")
                .isTrue();
        });
        executionStarted.await();
        interruptStream();
        assertExecutionOf(threadStillInterrupted::get).hasNoAssertionFailures();
    }

    private Runnable completeStreamOnceStarted(CountDownLatch streamStarted) {
        return () -> {
            try {
                assertThat(streamStarted)
                    .overridingErrorMessage("Stream should have been started")
                    .isDownBefore(100, MILLISECONDS);
            } finally {
                streamLog.onComplete();
            }
        };
    }

    private void interruptStream() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor)
            .overridingErrorMessage("Stream should have ended")
            .isShutedDownBefore(100, MILLISECONDS);
    }
}
