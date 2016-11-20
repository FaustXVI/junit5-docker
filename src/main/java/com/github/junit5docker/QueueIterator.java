package com.github.junit5docker;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.currentThread;

class QueueIterator implements Iterator<String>, AutoCloseable {
    private final AtomicBoolean opened;

    private final BlockingQueue<String> lines;

    private String lineRead;

    public QueueIterator(BlockingQueue<String> lines) {
        this.opened = new AtomicBoolean(true);
        this.lines = lines;
    }

    @Override
    public boolean hasNext() {
        while (opened.get() && lineRead == null) {
            try {
                lineRead = lines.poll(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                opened.set(false);
                currentThread().interrupt();
            }
        }
        return lineRead != null;
    }

    @Override
    public String next() {
        String lineRead = this.lineRead;
        this.lineRead = null;
        return lineRead;
    }

    @Override
    public void close() {
        opened.set(false);
    }
}
