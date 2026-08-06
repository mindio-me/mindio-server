-- 添加 notes 表的 project_id 字段，用于关联项目
-- 注意：不添加外键约束，约束逻辑在代码中实现

ALTER TABLE notes 
ADD COLUMN project_id BIGINT NULL;

CREATE INDEX idx_notes_project_id ON notes(project_id);

