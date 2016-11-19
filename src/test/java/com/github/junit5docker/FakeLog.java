package com.github.junit5docker;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

class FakeLog implements Iterator<String> {

    private final AtomicBoolean started;

    private boolean hasNext;

    private String waitedLog;

    private FakeLog(AtomicBoolean started, String waitedLog) {
        this.started = started;
        hasNext = true;
        this.waitedLog = waitedLog;
    }

    static Stream<String> fakeLog(AtomicBoolean started, String waitedLog) {
        return stream(spliteratorUnknownSize(new FakeLog(started, waitedLog), 0), false);
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public String next() {
        if (started.get()) {
            hasNext = false;
            return "19/11/2016 : " + waitedLog;
        }
        return "";
    }
}