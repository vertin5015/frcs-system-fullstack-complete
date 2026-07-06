-- 异步摘要与配额：在已有库上执行一次（若列已存在请注释对应行）
ALTER TABLE case_detail_info
    ADD COLUMN summary_status VARCHAR(32) NULL COMMENT 'PENDING/RUNNING/DONE/FAILED' AFTER content_en_US,
    ADD COLUMN summary_error TEXT NULL AFTER summary_status,
    ADD COLUMN summary_updated_at DATETIME NULL AFTER summary_error;

CREATE TABLE IF NOT EXISTS usage_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    case_id VARCHAR(255) NULL,
    action_type VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at)
) COMMENT='API usage log';
