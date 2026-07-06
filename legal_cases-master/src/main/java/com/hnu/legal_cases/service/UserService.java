package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.auth.ChangePasswordReqVO;
import com.hnu.legal_cases.dto.auth.LoginByCodeReqVO;
import com.hnu.legal_cases.dto.auth.ResetPasswordReqVO;
import com.hnu.legal_cases.dto.user.LoginReqVO;
import com.hnu.legal_cases.dto.user.LoginResVO;
import com.hnu.legal_cases.dto.user.RegisterReqVO;

public interface UserService {

    /**
     * 用户注册
     *
     * @param reqVO 用户注册入参
     */
    void register(RegisterReqVO reqVO);
    /**
     * 用户登录
     *
     * @param reqVO 用户登录入参
     * @return 用户登录出参
     */
    LoginResVO login(LoginReqVO reqVO);

    /**
     * 邮箱验证码登录
     */
    LoginResVO loginByCode(LoginByCodeReqVO reqVO);

    /**
     * 忘记密码：验证码 + 新密码
     */
    void resetPasswordByCode(ResetPasswordReqVO reqVO);

    /**
     * 修改密码：邮箱 + 旧密码 + 新密码
     */
    void changePassword(ChangePasswordReqVO reqVO);

    /**
     * 当前用户剩余 AI 摘要次数（游客返回 null）
     */
    Integer getSummaryCredits(Long userId);
}
