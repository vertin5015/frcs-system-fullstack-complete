CREATE DATABASE IF NOT EXISTS legal_cases
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE legal_cases;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(100) NOT NULL,
  registration_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_date DATETIME NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  summary_credits INT NOT NULL DEFAULT 3,
  insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_sources (
  source_id INT PRIMARY KEY,
  source_name VARCHAR(100) NOT NULL,
  source_url VARCHAR(512) NOT NULL,
  source_country VARCHAR(50) NOT NULL,
  insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_data_sources_country (source_country)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS case_en_US (
  pk BIGINT PRIMARY KEY AUTO_INCREMENT,
  case_id VARCHAR(255) NOT NULL,
  source_id INT NOT NULL,
  case_name TEXT NOT NULL,
  judgment_date VARCHAR(255) NULL,
  citation_count INT DEFAULT 0,
  summary TEXT NULL,
  original_document_url TEXT NULL,
  insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_case_en_case_id (case_id),
  KEY idx_case_en_source_id (source_id),
  KEY idx_case_en_judgment_date (judgment_date),
  CONSTRAINT fk_case_en_source FOREIGN KEY (source_id) REFERENCES data_sources(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS case_zh_CN (
  pk BIGINT PRIMARY KEY AUTO_INCREMENT,
  case_id VARCHAR(255) NOT NULL,
  source_id INT NOT NULL,
  case_name TEXT NOT NULL,
  judgment_date VARCHAR(255) NULL,
  citation_count INT DEFAULT 0,
  summary TEXT NULL,
  original_document_url TEXT NULL,
  insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_case_zh_case_id (case_id),
  KEY idx_case_zh_source_id (source_id),
  KEY idx_case_zh_judgment_date (judgment_date),
  CONSTRAINT fk_case_zh_case FOREIGN KEY (case_id) REFERENCES case_en_US(case_id) ON DELETE CASCADE,
  CONSTRAINT fk_case_zh_source FOREIGN KEY (source_id) REFERENCES data_sources(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS case_detail_info (
  pk BIGINT PRIMARY KEY AUTO_INCREMENT,
  case_id VARCHAR(255) NOT NULL,
  content_zh_CN LONGTEXT NULL,
  content_en_US LONGTEXT NULL,
  summary_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  summary_error TEXT NULL,
  summary_updated_at DATETIME NULL,
  insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_case_detail_case_id (case_id),
  KEY idx_case_detail_update_time (update_time),
  CONSTRAINT fk_case_detail_case FOREIGN KEY (case_id) REFERENCES case_en_US(case_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_favorites (
  favorite_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  case_id VARCHAR(255) NOT NULL,
  custom_name VARCHAR(255) NULL,
  tags VARCHAR(255) NULL,
  favorite_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite_user_case (user_id, case_id),
  KEY idx_favorite_user_id (user_id),
  KEY idx_favorite_case_id (case_id),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_favorite_case FOREIGN KEY (case_id) REFERENCES case_en_US(case_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS browse_history (
  history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  case_id VARCHAR(255) NOT NULL,
  browse_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_history_user_case (user_id, case_id),
  KEY idx_history_user_time (user_id, browse_time),
  KEY idx_history_case_id (case_id),
  CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_history_case FOREIGN KEY (case_id) REFERENCES case_en_US(case_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usage_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  case_id VARCHAR(255) NULL,
  action_type VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_usage_user_created_at (user_id, created_at),
  KEY idx_usage_case_id (case_id),
  CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recharge_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  package_id VARCHAR(64) NOT NULL,
  credits INT NOT NULL,
  amount_cents INT NOT NULL,
  currency VARCHAR(16) NOT NULL DEFAULT 'cny',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  channel VARCHAR(32) NULL,
  stripe_session_id VARCHAR(255) NULL,
  stripe_payment_intent VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at DATETIME NULL,
  remark VARCHAR(512) NULL,
  UNIQUE KEY uk_recharge_order_no (order_no),
  KEY idx_recharge_user_created_at (user_id, created_at),
  CONSTRAINT fk_recharge_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO data_sources (source_id, source_name, source_url, source_country)
VALUES
  (0, 'CourtListener', 'https://www.courtlistener.com/', 'US'),
  (1, 'EU Case Law', 'https://curia.europa.eu/', 'EU'),
  (2, 'Japanese Courts', 'https://www.courts.go.jp/', 'JPN')
ON DUPLICATE KEY UPDATE
  source_name = VALUES(source_name),
  source_url = VALUES(source_url),
  source_country = VALUES(source_country),
  update_time = NOW();
