package com.hnu.legal_cases.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SseStreamGuardTest {

    @Test
    void allowsOnlyOneTerminalTransitionAndOneLockRelease() {
        SseStreamGuard guard = new SseStreamGuard();

        assertThat(guard.tryTerminate()).isTrue();
        assertThat(guard.tryTerminate()).isFalse();
        assertThat(guard.tryReleaseLock()).isTrue();
        assertThat(guard.tryReleaseLock()).isFalse();
    }
}
