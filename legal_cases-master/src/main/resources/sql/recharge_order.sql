CREATE TABLE IF NOT EXISTS recharge_order (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no             VARCHAR(64)  NOT NULL UNIQUE COMMENT '业务订单号',
    user_id              BIGINT       NOT NULL,
    package_id           VARCHAR(32)  NOT NULL,
    credits              INT          NOT NULL,
    amount_cents         INT          NOT NULL,
    currency             VARCHAR(8)   NOT NULL DEFAULT 'cny',
    status               VARCHAR(16)  NOT NULL COMMENT 'PENDING,PAID,CANCELLED,FAILED',
    channel              VARCHAR(16)  NOT NULL COMMENT 'MOCK,STRIPE',
    stripe_session_id    VARCHAR(255),
    stripe_payment_intent VARCHAR(255),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at              TIMESTAMP NULL,
    remark               VARCHAR(512)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
