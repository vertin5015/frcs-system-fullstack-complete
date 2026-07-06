package com.hnu.legal_cases.dao;

import com.hnu.legal_cases.pojo.RechargeOrder;
import org.apache.ibatis.annotations.Param;

public interface RechargeOrderMapper {

    int insert(RechargeOrder row);

    RechargeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    int updatePaid(@Param("orderNo") String orderNo,
                   @Param("stripePaymentIntent") String stripePaymentIntent);

    int updateStripeSession(@Param("orderNo") String orderNo,
                            @Param("stripeSessionId") String stripeSessionId);
}
