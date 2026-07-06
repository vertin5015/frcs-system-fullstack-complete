package com.hnu.legal_cases.controller;

import com.alibaba.fastjson.JSON;
import com.hnu.legal_cases.dto.user.LoginReqVO;
import com.hnu.legal_cases.dto.user.LoginResVO;
import com.hnu.legal_cases.dto.user.RegisterReqVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.CheckReqVOService;
import com.hnu.legal_cases.service.UserService;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录与注册（推荐使用 POST + JSON，避免 GET 查询串对密码特殊字符解析错误）
 */
@Slf4j
@RestController
public class LoginController {
    @Autowired
    CheckReqVOService checkReqVOService;
    @Autowired
    UserService userService;

    @GetMapping(value = "/register", produces = "application/json")
    public JSONReturnBean<String> registerGet(@ModelAttribute RegisterReqVO reqVO) {
        return registerInternal(reqVO);
    }

    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<String> registerPost(@RequestBody RegisterReqVO reqVO) {
        return registerInternal(reqVO);
    }

    private JSONReturnBean<String> registerInternal(RegisterReqVO reqVO) {
        try {
            checkReqVOService.checkRegisterReqVO(reqVO);
            userService.register(reqVO);
            return JSONReturnBean.success("success");
        } catch (ServiceException e) {
            log.error("用户注册异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("用户注册错误，请稍后再试，error=", e);
            return JSONReturnBean.failed("用户注册错误，请稍后再试");
        }
    }

    @GetMapping(value = "/login", produces = "application/json")
    public JSONReturnBean<LoginResVO> loginGet(@ModelAttribute LoginReqVO reqVO) {
        return loginInternal(reqVO);
    }

    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<LoginResVO> loginPost(@RequestBody LoginReqVO reqVO) {
        return loginInternal(reqVO);
    }

    private JSONReturnBean<LoginResVO> loginInternal(LoginReqVO reqVO) {
        try {
            checkReqVOService.checkLoginReqVO(reqVO);
            LoginResVO resVO = userService.login(reqVO);
            return JSONReturnBean.success(resVO);
        } catch (ServiceException e) {
            log.error("用户登录异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("用户登录错误，请稍后再试，error=", e);
            return JSONReturnBean.failed(resolveLoginSystemError(e));
        }
    }

    /**
     * 将常见数据库/环境问题转为客户可读说明（完整堆栈已记日志）
     */
    private static String resolveLoginSystemError(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String m = t.getMessage();
            if (m == null) {
                continue;
            }
            String low = m.toLowerCase();
            if (low.contains("unknown column") && low.contains("summary_credits")) {
                return "登录失败：数据库 users 表缺少 summary_credits 字段，请在 MySQL 执行 legal_cases-master 中 src/main/resources/sql/alter_users_summary_credits.sql";
            }
            if (low.contains("communications link failure")
                    || (low.contains("could not create connection") && low.contains("mysql"))
                    || low.contains("connection refused")) {
                return "登录失败：无法连接 MySQL，请确认数据库已启动，并核对 application.yml 中的 url、用户名与密码";
            }
            if (low.contains("access denied for user")) {
                return "登录失败：MySQL 账号或密码错误（application.yml 与数据库权限不一致）";
            }
            if (low.contains("public key retrieval is not allowed")) {
                return "登录失败：MySQL 连接参数缺少 allowPublicKeyRetrieval=true，请检查 application.yml 的 spring.datasource.url";
            }
            if (low.contains("unknown database") && low.contains("legal_cases")) {
                return "登录失败：数据库 legal_cases 不存在，请先按 sql/schema.sql 创建库表";
            }
        }
        return "用户登录错误，请稍后再试（若刚改过库表或配置，请查看服务端控制台完整异常）";
    }
}
