package com.hnu.legal_cases.controller;

import com.hnu.legal_cases.util.JSONReturnBean;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void reportsUpWithoutDatabase() {
        HealthController controller = new HealthController();
        ReflectionTestUtils.setField(controller, "appName", "legal_cases");

        JSONReturnBean<Map<String, Object>> result = controller.health();

        assertThat(result.getCode()).isEqualTo(JSONReturnBean.SUCCESS_CODE);
        assertThat(result.getData())
                .containsEntry("status", "UP")
                .containsEntry("app", "legal_cases")
                .containsEntry("db", "UNKNOWN");
        assertThat(result.getData().get("uptimeSeconds")).isInstanceOf(Long.class);
    }
}
