-- 添加封面图片URL字段到视频表
-- 执行时间: 2025-12-07

ALTER TABLE vx_videos ADD COLUMN IF NOT EXISTS cover_url VARCHAR(500);

COMMENT ON COLUMN vx_videos.cover_url IS '封面图片URL';
