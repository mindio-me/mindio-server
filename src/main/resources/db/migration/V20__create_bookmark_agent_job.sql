CREATE TABLE bookmark_agent_job (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id        BIGINT       NOT NULL,
    type            VARCHAR(20)  NOT NULL COMMENT 'CLUSTER | TIMELINE',
    status          VARCHAR(20)  NOT NULL COMMENT 'RUNNING | DONE | FAILED',
    total_steps     INT          NOT NULL DEFAULT 0,
    completed_steps INT          NOT NULL DEFAULT 0,
    result_note_id  BIGINT       NULL,
    error_message   VARCHAR(500) NULL,
    created_at      DATETIME     NOT NULL,
    finished_at     DATETIME     NULL,
    CONSTRAINT fk_bookmark_agent_job_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);
CREATE INDEX ix_bookmark_agent_job_owner_type ON bookmark_agent_job (owner_id, type, created_at);

ALTER TABLE notes ADD COLUMN generated_type VARCHAR(20) NULL COMMENT 'CLUSTER | TIMELINE，NULL 表示普通笔记';
CREATE UNIQUE INDEX uk_notes_owner_generated_type ON notes (owner_id, generated_type);
