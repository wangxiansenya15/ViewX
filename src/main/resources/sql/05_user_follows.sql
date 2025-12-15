-- 用户关注关系表
-- 用于记录用户之间的关注关系，支持粉丝数和关注数统计

CREATE TABLE vx_user_follows (
    follower_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE, -- 关注者ID
    followed_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE, -- 被关注者ID
    created_at TIMESTAMP DEFAULT NOW(), -- 关注时间
    
    PRIMARY KEY (follower_id, followed_id),
    
    -- 防止自己关注自己
    CHECK (follower_id != followed_id)
);

-- 索引优化
-- 场景1：查询某用户的粉丝列表（谁关注了我）
CREATE INDEX idx_follows_followed_time ON vx_user_follows(followed_id, created_at DESC);

-- 场景2：查询某用户的关注列表（我关注了谁）
CREATE INDEX idx_follows_follower_time ON vx_user_follows(follower_id, created_at DESC);

-- 场景3：统计粉丝数（COUNT(*) WHERE followed_id = ?）
-- 场景4：统计关注数（COUNT(*) WHERE follower_id = ?）
-- 上述两个索引已经可以高效支持这些查询

-- 注释说明
COMMENT ON TABLE vx_user_follows IS '用户关注关系表';
COMMENT ON COLUMN vx_user_follows.follower_id IS '关注者用户ID';
COMMENT ON COLUMN vx_user_follows.followed_id IS '被关注者用户ID';
COMMENT ON COLUMN vx_user_follows.created_at IS '关注时间';
