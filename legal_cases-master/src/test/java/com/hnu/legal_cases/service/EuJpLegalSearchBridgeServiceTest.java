package com.hnu.legal_cases.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EuJpLegalSearchBridgeServiceTest {

    @Test
    void recognizesCourtListenerDeferredAntiBotResponse() {
        assertThat(EuJpLegalSearchBridgeService.isDeferredResponse(202)).isTrue();
        assertThat(EuJpLegalSearchBridgeService.isDeferredResponse(200)).isFalse();
    }

    /**
     * opinion URL 中的编号是 cluster_id，CourtListener v4 只接受 filterset 声明的过滤参数，
     * 传 cluster_id 会被判为未知参数并返回 HTTP 400；正确写法是 cluster__id（双下划线）。
     */
    @Test
    void buildsCourtListenerClusterFilterWithDoubleUnderscore() {
        String query = EuJpLegalSearchBridgeService.buildCourtListenerOpinionsQuery("9420161");

        assertThat(query).startsWith("?cluster__id=9420161");
        assertThat(query).contains("cluster__id=");
        assertThat(query).doesNotContain("cluster_id=");
    }
}
