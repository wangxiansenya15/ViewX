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

CREATE INDEX idx_comments_video_time ON vx_video_comments(video_id, created_at DESC);
CREATE INDEX idx_comments_parent ON vx_video_comments(parent_id);
CREATE INDEX idx_comments_user ON vx_video_comments(user_id, created_at DESC);

-- 优化点赞/收藏查询
-- 场景：查询"我点赞的视频"并按时间排序
CREATE INDEX idx_likes_user_time ON vx_video_likes(user_id, created_at DESC);
-- 场景：查询"视频的点赞用户列表" (可选，如果需要显示点赞头像)
CREATE INDEX idx_likes_video ON vx_video_likes(video_id);

-- 场景：查询"我收藏的视频"并按时间排序
CREATE INDEX idx_favorites_user_time ON vx_video_favorites(user_id, created_at DESC);
