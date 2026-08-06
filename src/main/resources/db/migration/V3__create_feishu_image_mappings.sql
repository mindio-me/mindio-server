-- 飞书图片映射表
-- 用于追踪导入时下载的图片及其映射关系

CREATE TABLE IF NOT EXISTS feishu_image_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 飞书标识
    file_token VARCHAR(255) NOT NULL UNIQUE COMMENT '飞书图片的file_token',
    block_id VARCHAR(128) COMMENT '图片所在的block_id',
    document_id VARCHAR(128) NOT NULL COMMENT '飞书文档ID',

    -- 关联
    snapshot_id BIGINT NOT NULL COMMENT '关联的快照ID',
    note_id BIGINT COMMENT '关联的笔记ID（可能为空）',
    attachment_id BIGINT COMMENT '关联的附件ID（上传后）',
    user_id BIGINT NOT NULL COMMENT '导入用户ID',

    -- 元数据
    width INT COMMENT '图片宽度（像素）',
    height INT COMMENT '图片高度（像素）',
    align INT COMMENT '对齐方式：1=居左, 2=居中, 3=居右',
    caption TEXT COMMENT '图片描述',

    -- 下载状态
    download_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '下载状态：pending, success, failed',
    download_error_message TEXT COMMENT '下载失败时的错误信息',
    feishu_url VARCHAR(1000) COMMENT '飞书临时下载URL',
    local_url VARCHAR(500) COMMENT '本地存储URL',

    -- 审计
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    downloaded_at TIMESTAMP COMMENT '下载完成时间',

    -- 索引
    INDEX idx_snapshot_id (snapshot_id),
    INDEX idx_note_id (note_id),
    INDEX idx_document_id (document_id),
    INDEX idx_user_id (user_id),
    INDEX idx_file_token (file_token),
    INDEX idx_download_status (download_status),

    -- 外键约束
    FOREIGN KEY (snapshot_id) REFERENCES feishu_document_snapshots(id) ON DELETE CASCADE,
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='飞书图片映射表 - 追踪导入时下载的图片';
