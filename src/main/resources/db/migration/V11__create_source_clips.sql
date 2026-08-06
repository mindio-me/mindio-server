-- 素材剪藏：存放从网页、公众号、AI对话导入的参考素材

CREATE TABLE source_clips (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_type VARCHAR(20)  NOT NULL COMMENT 'WEBPAGE | WECHAT_ARTICLE | AI_CHAT',
    source_url  VARCHAR(1000) NULL,
    source_title VARCHAR(500) NULL,
    source_author VARCHAR(200) NULL,
    ai_model    VARCHAR(100) NULL,
    title       VARCHAR(200) NOT NULL,
    content     LONGTEXT     NOT NULL,
    content_format VARCHAR(20) NULL COMMENT 'html | markdown | text',
    excerpt     TEXT         NULL,
    owner_id    BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_source_clips_owner (owner_id),
    KEY idx_source_clips_source_type (source_type),
    KEY idx_source_clips_created_at (created_at),
    CONSTRAINT fk_source_clips_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 素材与 Tag 的多对多关联
CREATE TABLE source_clip_tags (
    clip_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (clip_id, tag_id),
    CONSTRAINT fk_sct_clip FOREIGN KEY (clip_id) REFERENCES source_clips (id) ON DELETE CASCADE,
    CONSTRAINT fk_sct_tag  FOREIGN KEY (tag_id)  REFERENCES tags (id) ON DELETE CASCADE
);

-- 笔记与素材的关联（含批注和排序）
CREATE TABLE note_clip_refs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id     BIGINT NOT NULL,
    clip_id     BIGINT NOT NULL,
    user_note   TEXT   NULL,
    sort_order  INT    NOT NULL DEFAULT 0,
    linked_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_note_clip (note_id, clip_id),
    KEY idx_ncr_clip (clip_id),
    CONSTRAINT fk_ncr_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_ncr_clip FOREIGN KEY (clip_id) REFERENCES source_clips (id) ON DELETE CASCADE
);
