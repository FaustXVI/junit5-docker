package com.github.junit5docker;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

class QueueIterator implements Iterator<String>, AutoCloseable {

    private static final int POLL_TIMEOUT = 10;

    private final AtomicBoolean opened;

    private final BlockingQueue<String> lines;

    private Optional<String> lineRead = empty();

    QueueIterator(BlockingQueue<String> lines) {
        this.opened = new AtomicBoolean(true);
        this.lines = lines;
    }

    @Override
    public boolean hasNext() {
        while (opened.get() && !lineRead.isPresent()) {
            try {
                lineRead = ofNullable(lines.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                opened.set(false);
                currentThread().interrupt();
            }
        }
        return lineRead.isPresent();
    }

    @Override
    public String next() {
        if (!lineRead.isPresent()) throw new NoSuchElementException("Line read is null");
        String result = lineRead.get();
        lineRead = empty();
        return result;
    }

    @Override
    public void close() {
        opened.set(false);
    }
}
