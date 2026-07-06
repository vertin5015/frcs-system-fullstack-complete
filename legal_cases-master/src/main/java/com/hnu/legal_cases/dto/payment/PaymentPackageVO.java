package com.hnu.legal_cases.dto.payment;

import lombok.Data;

@Data
public class PaymentPackageVO {
    private String id;
    private int credits;
    private int priceCents;
    private String currency;
    private String label;
}
