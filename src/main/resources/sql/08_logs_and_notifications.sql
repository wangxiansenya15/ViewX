-- 用户行为日志表（用于大数据分析、画像构建）
CREATE TABLE vx_action_logs (
    id BIGINT PRIMARY KEY,
    user_id BIGINT, -- 可以为空（游客）
    action_type VARCHAR(20) NOT NULL, -- VIEW, LIKE, COMMENT, SHARE, LOGIN
    video_id BIGINT,
    ip_address VARCHAR(50),
    device_info VARCHAR(200),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 按照时间分区或分表通常是必要的，这里先建索引
CREATE INDEX idx_action_logs_user ON vx_action_logs(user_id);
CREATE INDEX idx_action_logs_video ON vx_action_logs(video_id);
CREATE INDEX idx_action_logs_time ON vx_action_logs(created_at DESC);

-- 消息通知表
CREATE TABLE vx_notifications (
    id BIGINT PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES vx_users(id), -- 接收者
    sender_id BIGINT REFERENCES vx_users(id), -- 发送者（触发动作的人）
    notification_type VARCHAR(20) NOT NULL, -- LIKE_VIDEO, COMMENT_VIDEO, REPLY_COMMENT, SYSTEM
    related_video_id BIGINT REFERENCES vx_videos(id), -- 关联视频
    related_comment_id BIGINT, -- 关联评论
    content TEXT, -- 通知内容快照
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    -- 软删除
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON vx_notifications(recipient_id, is_read);
CREATE INDEX idx_notifications_time ON vx_notifications(created_at DESC);
