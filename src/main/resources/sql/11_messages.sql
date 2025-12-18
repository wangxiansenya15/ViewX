-- ========================================
-- 私信聊天功能数据库表
-- ========================================

-- 消息表
CREATE TABLE vx_messages (
    id BIGINT PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,
    receiver_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'EMOJI')),
    is_read BOOLEAN DEFAULT FALSE,
    is_recalled BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    recalled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- 索引优化
    CONSTRAINT check_different_users CHECK (sender_id != receiver_id)
);

-- 索引
CREATE INDEX idx_messages_sender ON vx_messages(sender_id, created_at DESC);
CREATE INDEX idx_messages_receiver ON vx_messages(receiver_id, created_at DESC);
CREATE INDEX idx_messages_conversation ON vx_messages(sender_id, receiver_id, created_at DESC);
CREATE INDEX idx_messages_unread ON vx_messages(receiver_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_messages_is_deleted ON vx_messages(is_deleted);
CREATE INDEX idx_messages_is_recalled ON vx_messages(is_recalled);

-- 字段注释
COMMENT ON COLUMN vx_messages.is_recalled IS '是否已撤回';
COMMENT ON COLUMN vx_messages.is_deleted IS '是否已删除（软删除）';
COMMENT ON COLUMN vx_messages.recalled_at IS '撤回时间';

-- 会话表（用于快速查询会话列表）
CREATE TABLE vx_conversations (
    id BIGINT PRIMARY KEY,
    user1_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,
    user2_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,
    last_message_id BIGINT REFERENCES vx_messages(id) ON DELETE SET NULL,
    last_message_time TIMESTAMP DEFAULT NOW(),
    unread_count_user1 INT DEFAULT 0,
    unread_count_user2 INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- 确保每对用户只有一个会话（user1_id < user2_id）
    CONSTRAINT check_user_order CHECK (user1_id < user2_id),
    CONSTRAINT unique_conversation UNIQUE (user1_id, user2_id)
);

-- 索引
CREATE INDEX idx_conversations_user1 ON vx_conversations(user1_id, last_message_time DESC);
CREATE INDEX idx_conversations_user2 ON vx_conversations(user2_id, last_message_time DESC);
CREATE INDEX idx_conversations_last_message ON vx_conversations(last_message_time DESC);

-- 在线状态表（可选，也可以用 Redis）
CREATE TABLE vx_user_online_status (
    user_id BIGINT PRIMARY KEY REFERENCES vx_users(id) ON DELETE CASCADE,
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_online_status ON vx_user_online_status(is_online, last_seen DESC);

-- 触发器：自动更新 updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_messages_updated_at BEFORE UPDATE ON vx_messages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON vx_conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_online_status_updated_at BEFORE UPDATE ON vx_user_online_status
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
