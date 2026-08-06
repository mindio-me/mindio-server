-- 删除所有与 notes.id 相关的外键约束
-- 外键关联关系将在代码层面实现

-- 1. 删除 feishu_document_snapshots 表的 note_id 外键约束
-- 注意：MySQL 8.0.19+ 支持 IF EXISTS，旧版本需要先检查
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feishu_document_snapshots'
      AND CONSTRAINT_NAME = 'fk_feishu_snapshot_note'
      AND REFERENCED_TABLE_NAME = 'notes'
);

SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE feishu_document_snapshots DROP FOREIGN KEY fk_feishu_snapshot_note',
    'SELECT 1 AS "Constraint fk_feishu_snapshot_note does not exist"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 删除 feishu_image_mappings 表的 note_id 外键约束
-- 由于 V3 迁移脚本中使用的是匿名外键，需要先查找约束名称
SET @constraint_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feishu_image_mappings'
      AND COLUMN_NAME = 'note_id'
      AND REFERENCED_TABLE_NAME = 'notes'
      AND REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);

SET @sql = IF(@constraint_name IS NOT NULL,
    CONCAT('ALTER TABLE feishu_image_mappings DROP FOREIGN KEY ', @constraint_name),
    'SELECT 1 AS "No foreign key constraint found for feishu_image_mappings.note_id"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 删除 feishu_wiki_import_mappings 表中可能存在的 note_id 外键约束（如果存在）
SET @constraint_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feishu_wiki_import_mappings'
      AND COLUMN_NAME = 'note_id'
      AND REFERENCED_TABLE_NAME = 'notes'
      AND REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);

SET @sql = IF(@constraint_name IS NOT NULL,
    CONCAT('ALTER TABLE feishu_wiki_import_mappings DROP FOREIGN KEY ', @constraint_name),
    'SELECT 1 AS "No foreign key constraint found for feishu_wiki_import_mappings.note_id"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 删除所有其他可能由 Hibernate 自动创建的引用 notes.id 的外键约束
-- 查找并删除所有剩余的约束（使用存储过程方式）
-- 注意：由于 Flyway 的限制，这里使用动态 SQL 逐个处理
-- 如果还有其他表引用 notes.id，需要手动添加删除语句

-- 示例：如果有其他表（如 other_table）也有 note_id 外键，可以这样处理：
-- SET @constraint_name = (
--     SELECT CONSTRAINT_NAME
--     FROM information_schema.KEY_COLUMN_USAGE
--     WHERE TABLE_SCHEMA = DATABASE()
--       AND TABLE_NAME = 'other_table'
--       AND COLUMN_NAME = 'note_id'
--       AND REFERENCED_TABLE_NAME = 'notes'
--       AND REFERENCED_COLUMN_NAME = 'id'
--     LIMIT 1
-- );
-- SET @sql = IF(@constraint_name IS NOT NULL,
--     CONCAT('ALTER TABLE other_table DROP FOREIGN KEY ', @constraint_name),
--     'SELECT 1 AS "No constraint found"'
-- );
-- PREPARE stmt FROM @sql;
-- EXECUTE stmt;
-- DEALLOCATE PREPARE stmt;

-- 注意：索引 idx_note_id 保留，以提高查询性能
-- 外键约束已删除，但关联关系在代码层面维护

