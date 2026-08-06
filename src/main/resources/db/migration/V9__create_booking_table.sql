-- 预约表（TidyCal / 其他日程集成）
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '本地预约ID',
    external_id VARCHAR(191) NOT NULL COMMENT '第三方预约ID（如 TidyCal booking id）',
    name VARCHAR(200) COMMENT '预约人姓名',
    email VARCHAR(200) COMMENT '预约人邮箱',
    start_time DATETIME COMMENT '开始时间（UTC）',
    end_time DATETIME COMMENT '结束时间（UTC，可选）',
    timezone VARCHAR(100) COMMENT '时区字符串',
    event_slug VARCHAR(191) COMMENT '事件类型 / slug',
    raw_payload LONGTEXT COMMENT '完整原始 JSON 数据（便于调试）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_external_id (external_id),
    INDEX idx_email (email),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';

