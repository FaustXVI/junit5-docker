package com.github.junit5docker;

@FunctionalInterface
interface InterruptibleRunnable {
    void run() throws InterruptedException;

    static Runnable ignoreInterrupted(InterruptibleRunnable callable) {
        return () -> {
            try {
                callable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
