package com.github.junit5docker;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Thread.currentThread;
import static java.util.Spliterators.spliteratorUnknownSize;

class StreamLog extends LogContainerResultCallback {

    private BlockingQueue<String> lines = new ArrayBlockingQueue<>(1);

    private QueueIterator queueIterator = new QueueIterator(lines);

    @Override
    public void onNext(Frame item) {
        try {
            lines.put(new String(item.getPayload()));
        } catch (InterruptedException e) {
            currentThread().interrupt();
        }
    }

    @Override
    public void onComplete() {
        super.onComplete();
        queueIterator.close();
    }

    @Override
    public void onError(Throwable throwable) {
        super.onError(throwable);
        queueIterator.close();
    }

    public Stream<String> stream() {
        return StreamSupport.stream(spliteratorUnknownSize(queueIterator, 0), false);
    }
}
