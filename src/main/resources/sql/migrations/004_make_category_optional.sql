-- 将 category 字段改为可选，并设置默认值
-- 执行时间: 2025-12-07

ALTER TABLE vx_videos ALTER COLUMN category DROP NOT NULL;
ALTER TABLE vx_videos ALTER COLUMN category SET DEFAULT '其他';



COMMENT ON COLUMN vx_videos.category IS '视频分类，默认为"其他"';
