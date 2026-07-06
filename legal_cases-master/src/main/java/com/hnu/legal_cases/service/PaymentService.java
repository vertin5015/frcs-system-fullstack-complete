package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.payment.CreateOrderResultVO;
import com.hnu.legal_cases.dto.payment.PaymentPackageVO;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    List<PaymentPackageVO> listPackages(String language);

    CreateOrderResultVO createOrder(Long userId, String packageId);

    void mockConfirm(Long userId, String orderNo);

    /**
     * Stripe Webhook 或内部补单：将 PENDING 订单置为已支付并加次数
     *
     * @return 是否本次成功入账（幂等已付返回 false）
     */
    boolean fulfillPaidOrder(String orderNo, String externalPaymentId);

    Map<String, Object> channelInfo();
}
