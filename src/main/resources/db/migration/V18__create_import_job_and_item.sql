ALTER TABLE source_clips ADD COLUMN original_bookmarked_at DATETIME NULL;

CREATE TABLE import_job (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id      BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL COMMENT 'PARSING | CHECKING | READY | DONE | FAILED',
    file_name     VARCHAR(255) NULL,
    total_count   INT          NOT NULL DEFAULT 0,
    checked_count INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL,
    finished_at   DATETIME     NULL,
    CONSTRAINT fk_import_job_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE import_item (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id                BIGINT        NOT NULL,
    raw_title             VARCHAR(500)  NULL,
    raw_url               VARCHAR(2000) NOT NULL,
    normalized_url        VARCHAR(2000) NOT NULL,
    folder_path           VARCHAR(500)  NULL,
    bookmark_added_at     DATETIME      NULL,
    category              VARCHAR(20)   NOT NULL COMMENT 'NOISE | DUPLICATE | PENDING_CHECK | IMPORTABLE | DEAD_LINK',
    noise_reason          VARCHAR(30)   NULL,
    duplicate_of_clip_id  BIGINT        NULL,
    http_status           VARCHAR(20)   NULL,
    user_decision         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | CONFIRMED | SKIPPED',
    result_clip_id        BIGINT        NULL,
    CONSTRAINT fk_import_item_job FOREIGN KEY (job_id) REFERENCES import_job(id)
);
CREATE INDEX ix_import_item_job_category ON import_item (job_id, category);
