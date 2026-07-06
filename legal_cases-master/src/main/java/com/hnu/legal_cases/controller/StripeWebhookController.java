package com.hnu.legal_cases.controller;

import com.hnu.legal_cases.config.PaymentProperties;
import com.hnu.legal_cases.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Stripe webhook：需在 Dashboard 配置 endpoint，URL 与 app.payment.stripe.webhookSecret 一致
 */
@Slf4j
@RestController
@RequestMapping("/payment/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentProperties paymentProperties;
    private final PaymentService paymentService;

    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<String> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        PaymentProperties.Stripe st = paymentProperties.getStripe();
        if (!st.isEnabled() || !StringUtils.hasText(st.getWebhookSecret())) {
            return ResponseEntity.status(400).body("webhook disabled");
        }
        if (!StringUtils.hasText(sigHeader)) {
            return ResponseEntity.status(400).body("missing signature");
        }
        try {
            if (StringUtils.hasText(st.getSecretKey())) {
                Stripe.apiKey = st.getSecretKey();
            }
            Event event = Webhook.constructEvent(payload, sigHeader, st.getWebhookSecret());
            if ("checkout.session.completed".equals(event.getType())) {
                Optional<StripeObject> obj = event.getDataObjectDeserializer().getObject();
                if (obj.isPresent() && obj.get() instanceof Session session) {
                    String orderNo = session.getMetadata() != null ? session.getMetadata().get("orderNo") : null;
                    String pi = session.getPaymentIntent();
                    if (orderNo != null) {
                        paymentService.fulfillPaidOrder(orderNo, pi);
                    }
                }
            }
            return ResponseEntity.ok("");
        } catch (Exception e) {
            log.error("Stripe webhook 处理失败", e);
            return ResponseEntity.status(400).body("bad request");
        }
    }
}
