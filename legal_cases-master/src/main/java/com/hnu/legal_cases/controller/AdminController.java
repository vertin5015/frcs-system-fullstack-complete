package com.hnu.legal_cases.controller;

import com.hnu.legal_cases.config.AdminProperties;
import com.hnu.legal_cases.dao.UserMapper;
import com.hnu.legal_cases.dto.admin.AdminGrantCreditsReqVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理接口：需请求头 X-Admin-Secret 与 app.admin.secret 一致
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminProperties adminProperties;
    private final UserMapper userMapper;

    @PostMapping(value = "/grant-credits", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<String> grantCredits(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @RequestBody AdminGrantCreditsReqVO body) {
        try {
            if (!StringUtils.hasText(adminProperties.getSecret())) {
                return JSONReturnBean.failed("系统未配置 app.admin.secret，拒绝操作");
            }
            if (!adminProperties.getSecret().equals(secret)) {
                return JSONReturnBean.failed("无权限");
            }
            if (body == null || body.getUserId() == null || body.getCredits() == null) {
                return JSONReturnBean.failed("userId 与 credits 必填");
            }
            if (body.getCredits() <= 0) {
                return JSONReturnBean.failed("credits 须为正整数");
            }
            int n = userMapper.addSummaryCredits(body.getUserId(), body.getCredits());
            if (n == 0) {
                throw new ServiceException("用户不存在或更新失败");
            }
            log.info("管理员发放摘要次数 userId={} credits={} remark={}", body.getUserId(), body.getCredits(), body.getRemark());
            return JSONReturnBean.success("ok");
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("grantCredits", e);
            return JSONReturnBean.failed("操作失败");
        }
    }
}
