package com.hnu.legal_cases.controller;

import com.alibaba.fastjson.JSON;
import com.hnu.legal_cases.dto.auth.*;
import com.hnu.legal_cases.dto.user.LoginResVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.AuthCodeService;
import com.hnu.legal_cases.service.UserService;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码、忘记密码、修改密码等（在登录页使用）
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthCodeService authCodeService;
    @Autowired
    private UserService userService;

    @PostMapping(value = "/send-code", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<SendCodeResVO> sendCode(@RequestBody SendCodeReqVO reqVO) {
        try {
            SendCodeResVO res = authCodeService.sendCode(reqVO.getEmail(), reqVO.getPurpose());
            return JSONReturnBean.success(res);
        } catch (ServiceException e) {
            log.warn("send-code: {}", e.getMessage());
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("发送验证码异常, 入参={}", JSON.toJSONString(reqVO), e);
            return JSONReturnBean.failed("发送失败，请稍后重试");
        }
    }

    @PostMapping(value = "/login-by-code", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<LoginResVO> loginByCode(@RequestBody LoginByCodeReqVO reqVO) {
        try {
            LoginResVO res = userService.loginByCode(reqVO);
            return JSONReturnBean.success(res);
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("验证码登录异常, 入参={}", JSON.toJSONString(reqVO), e);
            return JSONReturnBean.failed("登录失败，请稍后重试");
        }
    }

    @PostMapping(value = "/reset-password", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<String> resetPassword(@RequestBody ResetPasswordReqVO reqVO) {
        try {
            userService.resetPasswordByCode(reqVO);
            return JSONReturnBean.success("ok");
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("重置密码异常", e);
            return JSONReturnBean.failed("重置失败，请稍后重试");
        }
    }

    @PostMapping(value = "/change-password", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<String> changePassword(@RequestBody ChangePasswordReqVO reqVO) {
        try {
            userService.changePassword(reqVO);
            return JSONReturnBean.success("ok");
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("修改密码异常", e);
            return JSONReturnBean.failed("修改失败，请稍后重试");
        }
    }
}
