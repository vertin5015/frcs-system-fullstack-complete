package com.hnu.legal_cases.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 管理接口密钥（发放次数等），生产环境务必修改 app.admin.secret
 */
@Data
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {
    private String secret = "";
}
