package com.hnu.legal_cases.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Bounds optional upstream model calls so local fallbacks can respond promptly. */
@Component
public class AiCallRunner {

    private final long timeoutMs;
    private final ExecutorService executor;

    @Autowired
    public AiCallRunner(@Value("${app.ai.call-timeout-ms:15000}") long timeoutMs) {
        this(timeoutMs, Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "ai-call");
            thread.setDaemon(true);
            return thread;
        }));
    }

    AiCallRunner(long timeoutMs, ExecutorService executor) {
        this.timeoutMs = Math.max(1, timeoutMs);
        this.executor = executor;
    }

    public <T> T call(Callable<T> action) throws Exception {
        Future<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return action.call();
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, executor);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
