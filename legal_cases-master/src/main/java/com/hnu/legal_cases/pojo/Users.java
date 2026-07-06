package com.hnu.legal_cases.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class Users {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 电子邮箱
     */
    private String email;
    /**
     * 注册日期
     */
    private Timestamp registrationDate;
    /**
     * 最后登录日期
     */
    private Timestamp lastLoginDate;
    /**
     * 是否激活（1：激活，0：未激活）
     */
    private Integer isActive;
    /**
     * AI 摘要剩余次数（注册赠送 + 后续购买）
     */
    private Integer summaryCredits;
    /**
     * 插入时间
     */
    private Timestamp insertTime;
    /**
     * 更新时间
     */
    private Timestamp updateTime;
}
