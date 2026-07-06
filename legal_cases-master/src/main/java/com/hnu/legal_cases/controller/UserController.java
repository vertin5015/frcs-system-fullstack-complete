package com.hnu.legal_cases.controller;

import com.hnu.legal_cases.service.UserService;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户权益（摘要次数等）
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping(value = "/summaryCredits", produces = "application/json")
    public JSONReturnBean<Integer> summaryCredits(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null || userId == 0L) {
                return JSONReturnBean.success(null);
            }
            Integer n = userService.getSummaryCredits(userId);
            return JSONReturnBean.success(n);
        } catch (Throwable e) {
            log.error("查询摘要次数失败 userId={}", userId, e);
            return JSONReturnBean.failed("查询摘要次数失败");
        }
    }
}
