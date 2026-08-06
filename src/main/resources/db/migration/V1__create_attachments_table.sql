-- 附件管理表
CREATE TABLE IF NOT EXISTS attachments (
    att_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    name VARCHAR(255) NOT NULL COMMENT '附件名称',
    att_dir VARCHAR(500) COMMENT '附件路径',
    satt_dir VARCHAR(500) COMMENT '压缩图片路径',
    att_size BIGINT COMMENT '附件大小（字节）',
    att_type VARCHAR(50) COMMENT '附件类型（如：png, jpg, pdf等）',
    pid INT DEFAULT 0 COMMENT '分类ID（0-编辑器, 1-笔记图片, 2-用户头像, 3-其他）',
    image_type INT DEFAULT 1 COMMENT '图片上传类型（1-本地, 2-七牛云, 3-OSS, 4-COS, 5-京东云）',
    owner BIGINT COMMENT '资源归属方（-1-平台, 其他为用户ID）',
    user_id BIGINT COMMENT '关联的用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_pid (pid),
    INDEX idx_owner (owner),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件管理表';

