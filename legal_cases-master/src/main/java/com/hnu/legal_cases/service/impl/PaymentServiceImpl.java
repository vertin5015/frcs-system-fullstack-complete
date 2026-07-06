package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.config.PaymentProperties;
import com.hnu.legal_cases.dao.RechargeOrderMapper;
import com.hnu.legal_cases.service.PaymentFulfillmentService;
import com.hnu.legal_cases.dto.payment.CreateOrderResultVO;
import com.hnu.legal_cases.dto.payment.PaymentPackageVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.RechargeOrder;
import com.hnu.legal_cases.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProperties paymentProperties;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final PaymentFulfillmentService paymentFulfillmentService;

    @Override
    public List<PaymentPackageVO> listPackages(String language) {
        boolean zh = language == null || language.startsWith("zh");
        return paymentProperties.getPackages().stream().map(p -> {
            PaymentPackageVO vo = new PaymentPackageVO();
            vo.setId(p.getId());
            vo.setCredits(p.getCredits());
            vo.setPriceCents(p.getPriceCents());
            vo.setCurrency(p.getCurrency());
            vo.setLabel(zh ? p.getLabelZh() : p.getLabelEn());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CreateOrderResultVO createOrder(Long userId, String packageId) {
        if (userId == null || userId == 0L) {
            throw new ServiceException("请先登录后再购买");
        }
        PaymentProperties.PackageItem pkg = findPackage(packageId);
        String orderNo = "RC" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        PaymentProperties.Stripe st = paymentProperties.getStripe();
        boolean tryStripe = st.isEnabled() && st.getSecretKey() != null && !st.getSecretKey().isBlank();

        if (tryStripe) {
            try {
                Stripe.apiKey = st.getSecretKey();
                String productName = pkg.getLabelZh();
                SessionCreateParams params = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(appendQuery(st.getSuccessUrl(), "orderNo", orderNo))
                        .setCancelUrl(st.getCancelUrl())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency(pkg.getStripeCurrency().toLowerCase())
                                                        .setUnitAmount((long) pkg.resolveStripeAmountCents())
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName(productName)
                                                                        .build())
                                                        .build())
                                        .build())
                        .putMetadata("orderNo", orderNo)
                        .putMetadata("userId", String.valueOf(userId))
                        .build();
                Session session = Session.create(params);

                RechargeOrder row = baseOrder(orderNo, userId, pkg, "PENDING", "STRIPE");
                row.setStripeSessionId(session.getId());
                rechargeOrderMapper.insert(row);

                CreateOrderResultVO res = new CreateOrderResultVO();
                res.setOrderNo(orderNo);
                res.setMockPay(false);
                res.setStripeCheckoutUrl(session.getUrl());
                return res;
            } catch (Exception e) {
                log.warn("Stripe Checkout 创建失败，将回退为模拟支付订单: {}", e.getMessage());
            }
        }

        RechargeOrder row = baseOrder(orderNo, userId, pkg, "PENDING", "MOCK");
        rechargeOrderMapper.insert(row);

        CreateOrderResultVO res = new CreateOrderResultVO();
        res.setOrderNo(orderNo);
        res.setMockPay(true);
        res.setStripeCheckoutUrl(null);
        return res;
    }

    private static String appendQuery(String base, String key, String value) {
        if (base == null) {
            return "?";
        }
        String sep = base.contains("?") ? "&" : "?";
        return base + sep + key + "=" + value;
    }

    private RechargeOrder baseOrder(String orderNo, Long userId, PaymentProperties.PackageItem pkg,
                                    String status, String channel) {
        RechargeOrder row = new RechargeOrder();
        row.setOrderNo(orderNo);
        row.setUserId(userId);
        row.setPackageId(pkg.getId());
        row.setCredits(pkg.getCredits());
        row.setAmountCents(pkg.getPriceCents());
        row.setCurrency(pkg.getCurrency());
        row.setStatus(status);
        row.setChannel(channel);
        return row;
    }

    private PaymentProperties.PackageItem findPackage(String packageId) {
        for (PaymentProperties.PackageItem p : paymentProperties.getPackages()) {
            if (p.getId().equals(packageId)) {
                return p;
            }
        }
        throw new ServiceException("无效的套餐: " + packageId);
    }

    @Override
    public void mockConfirm(Long userId, String orderNo) {
        RechargeOrder o = rechargeOrderMapper.selectByOrderNo(orderNo);
        if (o == null) {
            throw new ServiceException("订单不存在");
        }
        if (!o.getUserId().equals(userId)) {
            throw new ServiceException("订单不属于当前用户");
        }
        if (!"MOCK".equals(o.getChannel())) {
            throw new ServiceException("该订单请使用在线支付完成");
        }
        boolean ok = paymentFulfillmentService.fulfillPaidOrder(orderNo, "MOCK");
        if (!ok) {
            RechargeOrder again = rechargeOrderMapper.selectByOrderNo(orderNo);
            if (again != null && "PAID".equals(again.getStatus())) {
                return;
            }
            throw new ServiceException("确认失败，请稍后重试");
        }
    }

    @Override
    public boolean fulfillPaidOrder(String orderNo, String externalPaymentId) {
        return paymentFulfillmentService.fulfillPaidOrder(orderNo, externalPaymentId);
    }

    @Override
    public Map<String, Object> channelInfo() {
        PaymentProperties.Stripe st = paymentProperties.getStripe();
        Map<String, Object> m = new HashMap<>();
        m.put("mock", true);
        m.put("stripe", st.isEnabled() && st.getSecretKey() != null && !st.getSecretKey().isBlank());
        m.put("alipay", false);
        m.put("wechatPay", false);
        m.put("noteZh", "支付宝/微信需商户号与开放平台配置，当前未接入；可使用模拟支付或 Stripe。");
        m.put("noteEn", "Alipay/WeChat Pay require merchant onboarding; use mock pay or Stripe.");
        return m;
    }
}
