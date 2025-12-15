-- 话题表
CREATE TABLE vx_topics (
    id BIGINT PRIMARY KEY, -- 雪花算法ID
    
    -- 话题信息
    name VARCHAR(100) NOT NULL UNIQUE, -- 话题名称（不含#）
    description TEXT, -- 话题描述
    
    -- 统计信息
    video_count BIGINT DEFAULT 0, -- 使用该话题的视频数量
    view_count BIGINT DEFAULT 0, -- 话题总浏览量
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- 软删除
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- 视频-话题关联表
CREATE TABLE vx_video_topics (
    id BIGINT PRIMARY KEY, -- 雪花算法ID
    video_id BIGINT NOT NULL REFERENCES vx_videos(id) ON DELETE CASCADE,
    topic_id BIGINT NOT NULL REFERENCES vx_topics(id) ON DELETE CASCADE,
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT NOW(),
    
    -- 唯一约束：同一视频不能重复关联同一话题
    UNIQUE(video_id, topic_id)
);

-- 创建索引
CREATE INDEX idx_topics_name ON vx_topics(name);
CREATE INDEX idx_topics_video_count ON vx_topics(video_count DESC);
CREATE INDEX idx_topics_created_at ON vx_topics(created_at DESC);

CREATE INDEX idx_video_topics_video_id ON vx_video_topics(video_id);
CREATE INDEX idx_video_topics_topic_id ON vx_video_topics(topic_id);
