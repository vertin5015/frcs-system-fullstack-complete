-- 未执行前请先备份 users 表。新注册用户默认 3 次 AI 摘要额度（由应用配置 app.quota.summary-registration-credits 与列默认值共同约束）。
ALTER TABLE users
    ADD COLUMN summary_credits INT NOT NULL DEFAULT 3 COMMENT 'AI摘要剩余次数' AFTER is_active;
