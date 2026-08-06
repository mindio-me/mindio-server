-- 修复 feishu_image_mappings 表的索引问题
-- 问题：file_token 字段的索引键长度超过 MySQL InnoDB 的 767 字节限制
-- 原因：VARCHAR(255) * utf8mb4(4字节/字符) = 1020 字节 > 767 字节限制
-- 解决：使用前缀索引（191 字符，约 764 字节）

-- 删除旧的索引（如果存在）
-- 使用存储过程安全地删除索引，避免索引不存在时的错误
DROP PROCEDURE IF EXISTS drop_index_if_exists;

DELIMITER $$

CREATE PROCEDURE drop_index_if_exists()
BEGIN
    DECLARE index_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO index_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'feishu_image_mappings'
      AND index_name = 'idx_file_token';
    
    IF index_exists > 0 THEN
        ALTER TABLE feishu_image_mappings DROP INDEX idx_file_token;
    END IF;
END$$

DELIMITER ;

CALL drop_index_if_exists();
DROP PROCEDURE drop_index_if_exists;

-- 创建前缀索引（191 字符，utf8mb4 编码下约 764 字节，小于 767 字节限制）
-- 注意：唯一约束 uk_file_token 仍然使用完整字段
-- 如果唯一约束创建失败，可能需要：
-- 1. 启用 innodb_large_prefix（MySQL 5.7.7+ 默认启用）
-- 2. 设置 ROW_FORMAT=DYNAMIC（已在表定义中设置）
-- 3. 或者缩短 file_token 字段长度到 191 字符
CREATE INDEX idx_file_token ON feishu_image_mappings (file_token(191));

