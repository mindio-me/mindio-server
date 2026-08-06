-- 飞书文档原始数据快照表
CREATE TABLE IF NOT EXISTS feishu_document_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

    -- 关联关系
    mapping_id BIGINT NOT NULL COMMENT '关联 feishu_wiki_import_mappings.id',
    note_id BIGINT COMMENT '关联 notes.id（可能为空）',
    user_id BIGINT NOT NULL COMMENT '导入用户ID',

    -- 飞书文档标识
    document_id VARCHAR(128) NOT NULL COMMENT '飞书文档ID (obj_token)',
    document_revision_id BIGINT COMMENT '飞书文档版本号',

    -- 原始数据（两种格式都保存）
    raw_markdown LONGTEXT COMMENT 'raw_content API返回的markdown',
    raw_markdown_truncated BOOLEAN DEFAULT FALSE COMMENT '是否被截断',

    blocks_json LONGTEXT NOT NULL COMMENT 'blocks API返回的完整JSON',
    blocks_count INT DEFAULT 0 COMMENT 'blocks总数',
    blocks_pages INT DEFAULT 0 COMMENT '分页数',

    -- 元数据
    feishu_modified_time VARCHAR(64) COMMENT '飞书文档修改时间',
    content_size_bytes INT COMMENT '原始数据总大小（字节）',
    is_compressed BOOLEAN DEFAULT FALSE COMMENT '是否压缩存储',

    -- 转换结果（冗余存储，便于查询）
    converted_markdown LONGTEXT COMMENT '转换后的markdown',
    conversion_strategy VARCHAR(32) COMMENT 'raw_content | blocks_api | hybrid',

    -- 导入状态
    import_status VARCHAR(32) DEFAULT 'success' COMMENT 'success | partial | failed',
    import_error_message TEXT COMMENT '错误信息',

    -- 同步追踪（预留）
    sync_direction VARCHAR(32) COMMENT 'feishu_to_local | local_to_feishu',
    last_sync_at DATETIME COMMENT '最后同步时间',

    -- 审计
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_mapping_id (mapping_id),
    INDEX idx_note_id (note_id),
    INDEX idx_document_id (document_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_mapping_created (mapping_id, created_at DESC),
    INDEX idx_document_created (document_id, created_at DESC),

    CONSTRAINT fk_feishu_snapshot_mapping
        FOREIGN KEY (mapping_id) REFERENCES feishu_wiki_import_mappings(id) ON DELETE CASCADE,
    CONSTRAINT fk_feishu_snapshot_note
        FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE SET NULL,
    CONSTRAINT fk_feishu_snapshot_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='飞书文档原始数据快照 - 支持版本追踪和双向同步';

-- 扩展 feishu_wiki_import_mappings 表
ALTER TABLE feishu_wiki_import_mappings
ADD COLUMN latest_snapshot_id BIGINT COMMENT '最新快照ID',
ADD COLUMN total_snapshots INT DEFAULT 0 COMMENT '快照总数',
ADD COLUMN sync_enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用同步';

-- 添加外键约束（在表创建后）
ALTER TABLE feishu_wiki_import_mappings
ADD CONSTRAINT fk_feishu_mapping_latest_snapshot
    FOREIGN KEY (latest_snapshot_id) REFERENCES feishu_document_snapshots(id) ON DELETE SET NULL;
