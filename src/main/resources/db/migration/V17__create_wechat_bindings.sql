-- 微信聊天导入：openid 与 mindio 用户的绑定关系（一次性绑定码配对）
CREATE TABLE wechat_bindings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    bind_code       VARCHAR(10) NULL,
    openid          VARCHAR(64) NULL,
    user_id         BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL COMMENT 'PENDING | BOUND | EXPIRED',
    code_expires_at DATETIME NULL,
    bound_at        DATETIME NULL,
    created_at      DATETIME NOT NULL,
    CONSTRAINT fk_wechat_binding_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- PENDING 记录 openid 为 NULL，MySQL 唯一索引允许多个 NULL 并存；
-- 一旦 openid 写入（绑定完成），该值在全表内即唯一。
CREATE UNIQUE INDEX ux_wechat_binding_openid ON wechat_bindings (openid);
CREATE INDEX ix_wechat_binding_code ON wechat_bindings (bind_code, status);
