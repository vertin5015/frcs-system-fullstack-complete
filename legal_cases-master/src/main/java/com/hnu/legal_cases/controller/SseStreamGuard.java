package com.hnu.legal_cases.controller;

import java.util.concurrent.atomic.AtomicBoolean;

/** Tracks the terminal state of an asynchronous SSE request. */
final class SseStreamGuard {

    private final AtomicBoolean terminal = new AtomicBoolean(false);
    private final AtomicBoolean lockReleased = new AtomicBoolean(false);
    private final AtomicBoolean clientClosed = new AtomicBoolean(false);

    boolean tryTerminate() {
        return terminal.compareAndSet(false, true);
    }

    boolean tryReleaseLock() {
        return lockReleased.compareAndSet(false, true);
    }

    boolean isClosed() {
        return terminal.get() || clientClosed.get();
    }

    void markClientClosed() {
        clientClosed.set(true);
        terminal.set(true);
    }
}
