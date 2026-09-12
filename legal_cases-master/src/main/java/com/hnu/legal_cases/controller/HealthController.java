package com.hnu.legal_cases.controller;

import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维健康检查接口，供部署流水线 / Nginx 探活使用。
 * <p>
 * 注意：后端配置了 {@code server.servlet.context-path=/api}，所以容器内的实际访问地址是
 * {@code GET /api/health}；宿主机 Nginx 侧另外暴露了 {@code GET /health} 作为统一探活入口。
 * 只要进程存活就返回 HTTP 200，数据库等依赖的状态放在 data 里，方便快速定位是容器挂了还是依赖挂了。
 */
@Slf4j
@RestController
public class HealthController {

    private final long startedAt = System.currentTimeMillis();

    @Autowired(required = false)
    private DataSource dataSource;

    @Value("${spring.application.name:legal_cases}")
    private String appName;

    @GetMapping(value = "/health", produces = "application/json")
    public JSONReturnBean<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("app", appName);
        data.put("uptimeSeconds", (System.currentTimeMillis() - startedAt) / 1000);
        data.put("db", probeDatabase());
        data.put("time", OffsetDateTime.now().toString());
        return JSONReturnBean.success(data);
    }

    private String probeDatabase() {
        if (dataSource == null) {
            return "UNKNOWN";
        }
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Throwable e) {
            log.warn("health check: database unreachable: {}", e.getMessage());
            return "DOWN";
        }
    }
}
