package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dao.RechargeOrderMapper;
import com.hnu.legal_cases.dao.UserMapper;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.RechargeOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 独立 Bean，保证从 Webhook / 模拟支付 调用时事务生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFulfillmentService {

    private final RechargeOrderMapper rechargeOrderMapper;
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public boolean fulfillPaidOrder(String orderNo, String externalPaymentId) {
        RechargeOrder o = rechargeOrderMapper.selectByOrderNo(orderNo);
        if (o == null) {
            log.warn("fulfillPaidOrder 订单不存在 orderNo={}", orderNo);
            return false;
        }
        if ("PAID".equals(o.getStatus())) {
            return false;
        }
        if (!"PENDING".equals(o.getStatus())) {
            log.warn("fulfillPaidOrder 状态非 PENDING orderNo={} status={}", orderNo, o.getStatus());
            return false;
        }
        int n = rechargeOrderMapper.updatePaid(orderNo, externalPaymentId);
        if (n == 0) {
            return false;
        }
        int u = userMapper.addSummaryCredits(o.getUserId(), o.getCredits());
        if (u == 0) {
            throw new ServiceException("入账失败：用户不存在");
        }
        return true;
    }
}
