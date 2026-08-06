-- 收藏夹收窄为网页/公众号 URL 导入，去掉 AI_CHAT 与手动录入；
-- 新增 extraction_mode/extraction_status 支持"仅保存链接"和抓取失败降级。

ALTER TABLE source_clips
  ADD COLUMN extraction_mode VARCHAR(20) NOT NULL DEFAULT 'FULL' COMMENT 'FULL | LINK_ONLY',
  ADD COLUMN extraction_status VARCHAR(20) NULL COMMENT 'SUCCESS | FAILED，仅 extraction_mode=FULL 时有意义',
  MODIFY COLUMN content LONGTEXT NULL;

UPDATE source_clips SET extraction_status = 'SUCCESS' WHERE content IS NOT NULL;

-- 现无重要 AI_CHAT 存量数据；source_clip_tags/note_clip_refs 对 source_clips 都有
-- ON DELETE CASCADE 外键（见 V11），删除父行会自动级联清理。
DELETE FROM source_clips WHERE source_type = 'AI_CHAT';
