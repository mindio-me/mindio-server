-- 为项目表添加正文内容字段
-- content: 存储富文本正文内容
-- content_type: 标识内容类型（richtext, markdown等）

ALTER TABLE projects
ADD COLUMN content LONGTEXT COMMENT '项目正文内容（富文本）',
ADD COLUMN content_type VARCHAR(20) NOT NULL DEFAULT 'richtext' COMMENT '内容类型：richtext, markdown等';

