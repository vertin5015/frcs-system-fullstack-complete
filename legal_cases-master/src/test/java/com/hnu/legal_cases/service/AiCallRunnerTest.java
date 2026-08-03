package com.hnu.legal_cases.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCallRunnerTest {

    @Test
    void timesOutSlowModelCall() {
        AiCallRunner runner = new AiCallRunner(20, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ai-call-test");
            thread.setDaemon(true);
            return thread;
        }));

        assertThatThrownBy(() -> runner.call(() -> {
            Thread.sleep(200);
            return "late";
        })).isInstanceOf(java.util.concurrent.TimeoutException.class);
    }
}
