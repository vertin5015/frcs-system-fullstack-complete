package com.hnu.legal_cases.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private List<PackageItem> packages = new ArrayList<>();
    private Stripe stripe = new Stripe();

    @Data
    public static class PackageItem {
        private String id;
        private int credits;
        private int priceCents;
        private String currency = "cny";
        private String labelZh = "";
        private String labelEn = "";
        /**
         * Stripe Checkout 使用的金额（最小货币单位）；未配置时回退为 priceCents
         */
        private Integer stripeAmountCents;
        /**
         * Stripe 结算货币，测试环境常用 usd
         */
        private String stripeCurrency = "usd";

        public int resolveStripeAmountCents() {
            if (stripeAmountCents != null && stripeAmountCents > 0) {
                return stripeAmountCents;
            }
            return priceCents;
        }
    }

    @Data
    public static class Stripe {
        private boolean enabled;
        private String secretKey = "";
        private String webhookSecret = "";
        private String successUrl = "";
        private String cancelUrl = "";
    }
}
