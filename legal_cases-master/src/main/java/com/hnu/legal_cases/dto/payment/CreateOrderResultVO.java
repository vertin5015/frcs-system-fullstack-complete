package com.hnu.legal_cases.dto.payment;

import lombok.Data;

@Data
public class CreateOrderResultVO {
    private String orderNo;
    private boolean mockPay;
    private String stripeCheckoutUrl;
}
