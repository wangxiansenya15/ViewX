-- 创建话题表和视频-话题关联表
-- 执行时间: 2025-12-07

-- 话题表
CREATE TABLE IF NOT EXISTS vx_topics (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    video_count BIGINT DEFAULT 0,
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- 视频-话题关联表
CREATE TABLE IF NOT EXISTS vx_video_topics (
    id BIGINT PRIMARY KEY,
    video_id BIGINT NOT NULL REFERENCES vx_videos(id) ON DELETE CASCADE,
    topic_id BIGINT NOT NULL REFERENCES vx_topics(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(video_id, topic_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_topics_name ON vx_topics(name);
CREATE INDEX IF NOT EXISTS idx_topics_video_count ON vx_topics(video_count DESC);
CREATE INDEX IF NOT EXISTS idx_topics_created_at ON vx_topics(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_video_topics_video_id ON vx_video_topics(video_id);
CREATE INDEX IF NOT EXISTS idx_video_topics_topic_id ON vx_video_topics(topic_id);
