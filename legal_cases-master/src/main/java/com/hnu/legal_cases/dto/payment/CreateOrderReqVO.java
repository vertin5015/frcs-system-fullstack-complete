package com.hnu.legal_cases.dto.payment;

import lombok.Data;

@Data
public class CreateOrderReqVO {
    private Long userId;
    private String packageId;
}
