CREATE TABLE achievements (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id       BIGINT        NOT NULL,
    title          VARCHAR(200)  NOT NULL,
    subtitle       VARCHAR(200)  NULL,
    type           VARCHAR(100)  NULL COMMENT '自由文本，如：独立产品、比赛获奖、实习成果',
    status         VARCHAR(20)   NOT NULL DEFAULT 'active' COMMENT 'active | done',
    icon           VARCHAR(100)  NULL,
    icon_variant   VARCHAR(20)   NOT NULL DEFAULT 'purple',
    description    LONGTEXT      NOT NULL,
    technologies   LONGTEXT      NULL COMMENT '逗号分隔的技术栈标签',
    is_active      TINYINT(1)    NOT NULL DEFAULT 1,
    display_order  INT           NOT NULL DEFAULT 0,
    created_at     DATETIME      NOT NULL,
    modified_at    DATETIME      NOT NULL,
    CONSTRAINT fk_achievements_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);
CREATE INDEX ix_achievements_active_order ON achievements (is_active, display_order);
