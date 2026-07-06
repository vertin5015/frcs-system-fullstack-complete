-- 涉外法律案例聚合网站数据库模型
-- 创建日期: 2025-06-25

create database legal_cases;
-- 用户表
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户唯一标识ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '用户密码，存储加密后的哈希值',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '用户邮箱，用于登录',
    registration_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用户注册时间',
    last_login_date DATETIME COMMENT '用户最后登录时间',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '账号是否激活状态',
    summary_credits INT NOT NULL DEFAULT 3 COMMENT 'AI摘要剩余次数',
    insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    INDEX idx_email (email) COMMENT '邮箱索引，提高邮箱查询效率',
    INDEX idx_username (username) COMMENT '用户名索引，提高用户名查询效率'
) COMMENT '存储系统用户信息'  collate = utf8mb4_unicode_ci;;

-- 数据源表
CREATE TABLE data_sources (
    source_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '数据源唯一标识ID',
    source_name VARCHAR(100) NOT NULL COMMENT '数据源名称，如CourtListener、欧盟司法法院判例库等',
    source_url VARCHAR(255) NOT NULL COMMENT '数据源网站URL',
    source_country VARCHAR(50) NOT NULL COMMENT '数据源所属国家或地区',
    insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    INDEX idx_country (source_country) COMMENT '国家索引，便于按国家筛选数据源'
) COMMENT '存储案例数据来源信息' collate = utf8mb4_unicode_ci;;

-- 案例基本信息表（英文）
CREATE TABLE case_en_US (
    pk BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    case_id VARCHAR(255) UNIQUE NOT NULL COMMENT '案例ID',
    source_id INT NOT NULL COMMENT '数据源ID',
    case_name TEXT NOT NULL COMMENT '案例名称',
    judgment_date VARCHAR(255) COMMENT '判决日期',
    citation_count INT DEFAULT 0 COMMENT '引用次数',
    summary TEXT COMMENT '摘要',
    original_document_url TEXT COMMENT '原始判决文书URL',
    insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    INDEX idx_case_id (case_id) COMMENT '案例ID索引',
    INDEX idx_judgment_date (judgment_date) COMMENT '判决日期索引'
) COMMENT '存储案例基本信息' collate = utf8mb4_unicode_ci;;

-- 案例基本信息表(中文)
CREATE TABLE case_zh_CN (
    pk BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    case_id VARCHAR(255) NOT NULL COMMENT '案例ID',
    source_id INT NOT NULL COMMENT '数据源ID',
    case_name TEXT NOT NULL COMMENT '案例名称',
    judgment_date VARCHAR(255) COMMENT '判决日期',
    citation_count INT DEFAULT 0 COMMENT '引用次数',
    summary TEXT COMMENT '摘要',
    original_document_url TEXT COMMENT '原始判决文书URL',
    insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    INDEX idx_case_id (case_id) COMMENT '案例ID索引',
    INDEX idx_judgment_date (judgment_date) COMMENT '判决日期索引',
    FOREIGN KEY (case_id) REFERENCES case_en_US(case_id) ON DELETE CASCADE
) COMMENT '存储案例基本信息' collate = utf8mb4_unicode_ci;;

-- 案例详细信息表
CREATE TABLE case_detail_info (
    pk BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    case_id VARCHAR(255) NOT NULL COMMENT '案例ID',
    content_zh_CN LONGTEXT COMMENT '中文详细信息',
    content_en_US LONGTEXT COMMENT '英文详细信息',
    insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    INDEX idx_case_id (case_id) COMMENT '案例ID索引',
    FOREIGN KEY (case_id) REFERENCES case_en_US(case_id) ON DELETE CASCADE
) COMMENT '存储案例详细信息' collate = utf8mb4_unicode_ci;;

-- 用户收藏表
CREATE TABLE user_favorites (
    favorite_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏记录唯一标识ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    case_id VARCHAR(255) NOT NULL COMMENT '案例ID',
    custom_name VARCHAR(255) COMMENT '用户自定义的案例名称',
    tags VARCHAR(255) COMMENT '用户添加的标签，以逗号分隔',
    favorite_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (case_id) REFERENCES case_en_US(case_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_case (user_id, case_id) COMMENT '确保用户不会重复收藏同一案例',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引，便于按用户查询收藏'
) COMMENT '存储用户收藏的案例信息' collate = utf8mb4_unicode_ci;;

-- 浏览历史表
CREATE TABLE browse_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '浏览历史记录唯一标识ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    case_id VARCHAR(255) NOT NULL COMMENT '浏览的案例ID',
    browse_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    insert_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (case_id) REFERENCES case_en_US(case_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_case (user_id, case_id) COMMENT '用户和案例ID的唯一组合，确保每个用户对每个案例只有一条浏览记录',
    INDEX idx_user_time (user_id, browse_time) COMMENT '用户和浏览时间复合索引，便于查询用户的浏览历史',
    INDEX idx_case_id (case_id) COMMENT '案例ID索引，便于查询与特定案例相关的浏览'
) COMMENT '存储用户的浏览历史记录' collate = utf8mb4_unicode_ci;;