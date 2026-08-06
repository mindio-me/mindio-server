-- LinkedIn OAuth binding (per user) and publish audit logs

CREATE TABLE linkedin_user_auth (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    member_id VARCHAR(128) NOT NULL,
    person_urn VARCHAR(256) NOT NULL,
    access_token_enc LONGTEXT NOT NULL,
    refresh_token_enc LONGTEXT,
    expires_at DATETIME(6) NULL,
    scope VARCHAR(512) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_linkedin_user_auth_user (user_id),
    CONSTRAINT fk_linkedin_user_auth_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE linkedin_publish_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    post_urn VARCHAR(512) NULL,
    commentary LONGTEXT NULL,
    visibility VARCHAR(32) NULL,
    error_message LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_linkedin_publish_note (note_id),
    KEY idx_linkedin_publish_user (user_id),
    CONSTRAINT fk_linkedin_publish_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_linkedin_publish_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
