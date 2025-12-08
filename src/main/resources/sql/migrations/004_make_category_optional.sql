-- 将 category 字段改为可选，并设置默认值
-- 执行时间: 2025-12-07

ALTER TABLE vx_videos ALTER COLUMN category DROP NOT NULL;
ALTER TABLE vx_videos ALTER COLUMN category SET DEFAULT '其他';

-- 更新现有的 NULL 值（如果有）
UPDATE vx_videos SET category = '其他' WHERE category IS NULL;

COMMENT ON COLUMN vx_videos.category IS '视频分类，默认为"其他"';
