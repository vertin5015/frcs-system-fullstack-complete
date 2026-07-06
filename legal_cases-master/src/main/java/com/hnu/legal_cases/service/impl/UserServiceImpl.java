package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.UserMapper;
import com.hnu.legal_cases.dto.auth.ChangePasswordReqVO;
import com.hnu.legal_cases.dto.auth.LoginByCodeReqVO;
import com.hnu.legal_cases.dto.auth.ResetPasswordReqVO;
import com.hnu.legal_cases.dto.user.LoginReqVO;
import com.hnu.legal_cases.dto.user.LoginResVO;
import com.hnu.legal_cases.dto.user.RegisterReqVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.Users;
import com.hnu.legal_cases.service.AuthCodeService;
import com.hnu.legal_cases.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AuthCodeService authCodeService;

    private static final String PURPOSE_LOGIN = "LOGIN";
    private static final String PURPOSE_RESET = "RESET";

    private void assertStrongPassword(String p) {
        if (p == null || p.length() < 8) {
            throw new ServiceException("密码至少8位");
        }
        if (!p.matches("^(?=.*[A-Za-z])(?=.*\\d)[\\w!@#$%^&*()_+\\-=.]{8,}$")) {
            throw new ServiceException("密码需同时包含字母和数字");
        }
    }

    /**
     * 用户注册
     *
     * @param reqVO 用户注册入参
     */
    @Override
    public void register(RegisterReqVO reqVO) {
        String username = reqVO.getUsername();
        String email = reqVO.getEmail();
        String password = reqVO.getPassword();

        boolean usernameExists = userMapper.existsByUsername(username);
        if (usernameExists) {
            throw new ServiceException("用户名已存在");
        }
        boolean emailExists = userMapper.existsByEmail(email);
        if (emailExists) {
            throw new ServiceException("邮箱已存在");
        }

        userMapper.insert(username, email, password);
        log.info("用户注册成功，username={}", username);
    }

    /**
     * 用户登录
     *
     * @param reqVO 用户登录入参
     * @return 用户登录出参
     */
    @Override
    public LoginResVO login(LoginReqVO reqVO) {
        String email = reqVO.getEmail();
        String password = reqVO.getPassword();

        Users user = userMapper.loginByEmail(email, password);
        if (user == null) {
            throw new ServiceException("登录失败");
        }

        LoginResVO resVO = new LoginResVO();
        resVO.setUserId(user.getUserId());
        resVO.setUsername(user.getUsername());
        resVO.setSummaryCredits(user.getSummaryCredits());

        userMapper.updateLastLoginDate(user.getUserId());
        log.info("用户登录成功，username={}", user.getUsername());

        return resVO;
    }

    @Override
    public LoginResVO loginByCode(LoginByCodeReqVO reqVO) {
        authCodeService.verifyAndDelete(reqVO.getEmail().trim(), PURPOSE_LOGIN, reqVO.getCode());
        Users user = userMapper.findActiveByEmail(reqVO.getEmail().trim());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        LoginResVO resVO = new LoginResVO();
        resVO.setUserId(user.getUserId());
        resVO.setUsername(user.getUsername());
        resVO.setSummaryCredits(user.getSummaryCredits());
        userMapper.updateLastLoginDate(user.getUserId());
        log.info("验证码登录成功，username={}", user.getUsername());
        return resVO;
    }

    @Override
    public void resetPasswordByCode(ResetPasswordReqVO reqVO) {
        assertStrongPassword(reqVO.getNewPassword());
        authCodeService.verifyAndDelete(reqVO.getEmail().trim(), PURPOSE_RESET, reqVO.getCode());
        int n = userMapper.updatePasswordByEmail(reqVO.getEmail().trim(), reqVO.getNewPassword());
        if (n == 0) {
            throw new ServiceException("重置失败，用户不存在或未激活");
        }
        log.info("忘记密码重置成功 email={}", reqVO.getEmail());
    }

    @Override
    public void changePassword(ChangePasswordReqVO reqVO) {
        assertStrongPassword(reqVO.getNewPassword());
        Users u = userMapper.loginByEmail(reqVO.getEmail().trim(), reqVO.getOldPassword());
        if (u == null) {
            throw new ServiceException("邮箱或旧密码错误");
        }
        if (reqVO.getOldPassword().equals(reqVO.getNewPassword())) {
            throw new ServiceException("新密码不能与旧密码相同");
        }
        int n = userMapper.updatePasswordByEmail(reqVO.getEmail().trim(), reqVO.getNewPassword());
        if (n == 0) {
            throw new ServiceException("修改失败");
        }
        log.info("修改密码成功 email={}", reqVO.getEmail());
    }

    @Override
    public Integer getSummaryCredits(Long userId) {
        if (userId == null || userId == 0L) {
            return null;
        }
        return userMapper.selectSummaryCreditsByUserId(userId);
    }
}
