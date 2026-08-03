package com.hnu.legal_cases.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCodeServiceImplTest {

    @Test
    void masksEmailForAuthenticationLogs() {
        assertThat(AuthCodeServiceImpl.maskEmail("1245788683@qq.com"))
                .isEqualTo("124****683@qq.com");
        assertThat(AuthCodeServiceImpl.maskEmail("a@b.com"))
                .isEqualTo("*@b.com");
    }
}
