package com.hnu.legal_cases.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaseCacheServiceImplTest {

    @Test
    void parsesIsoAndSlashDatesForCacheOrdering() {
        assertThat(CaseCacheServiceImpl.parseDateToScore("2023-01-01"))
                .isGreaterThan(0.0);
        assertThat(CaseCacheServiceImpl.parseDateToScore("2023/01/01"))
                .isEqualTo(CaseCacheServiceImpl.parseDateToScore("2023-01-01"));
    }
}
