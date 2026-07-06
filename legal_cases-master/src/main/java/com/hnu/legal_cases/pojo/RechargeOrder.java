package com.hnu.legal_cases.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class RechargeOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private String packageId;
    private Integer credits;
    private Integer amountCents;
    private String currency;
    private String status;
    private String channel;
    private String stripeSessionId;
    private String stripePaymentIntent;
    private Timestamp createdAt;
    private Timestamp paidAt;
    private String remark;
}
