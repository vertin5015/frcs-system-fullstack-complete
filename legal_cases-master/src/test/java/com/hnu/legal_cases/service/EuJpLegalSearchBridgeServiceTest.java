package com.hnu.legal_cases.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EuJpLegalSearchBridgeServiceTest {

    @Test
    void recognizesCourtListenerDeferredAntiBotResponse() {
        assertThat(EuJpLegalSearchBridgeService.isDeferredResponse(202)).isTrue();
        assertThat(EuJpLegalSearchBridgeService.isDeferredResponse(200)).isFalse();
    }
}
