-- 点赞记录表
CREATE TABLE vx_video_likes (
    user_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,
    video_id BIGINT NOT NULL REFERENCES vx_videos(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, video_id)
);

-- 收藏记录表
CREATE TABLE vx_video_favorites (
    user_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,
    video_id BIGINT NOT NULL REFERENCES vx_videos(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, video_id)
);

-- 评论表
CREATE TABLE vx_video_comments (
    id BIGINT PRIMARY KEY, -- 雪花算法ID
    video_id BIGINT NOT NULL REFERENCES vx_videos(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,
    parent_id BIGINT, -- 父评论ID，用于回复
    content TEXT NOT NULL,
    like_count INTEGER DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- 软删除
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- 注释：索引已移至 13_indexes_optimization.sql 统一管理
