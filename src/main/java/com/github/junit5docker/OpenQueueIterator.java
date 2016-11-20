package com.github.junit5docker;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.currentThread;

class OpenQueueIterator implements Iterator<String> {
    private final AtomicBoolean opened;

    private final BlockingQueue<String> lines;

    private String lineRead;

    public OpenQueueIterator(AtomicBoolean opened, BlockingQueue<String> lines) {
        this.opened = opened;
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
}
