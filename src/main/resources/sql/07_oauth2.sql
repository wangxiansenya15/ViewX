-- 第三方登录关联表
CREATE TABLE vx_social_users (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,
    
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GITHUB', 'GOOGLE', 'QQ', 'WECHAT', 'APPLE')),
    provider_user_id VARCHAR(100) NOT NULL, -- 第三方平台的唯一ID (openid/sub)
    
    -- 第三方信息快照
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    email VARCHAR(100),
    
    access_token TEXT, -- 可选，如果需要调用第三方API
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(provider, provider_user_id), -- 防止同一个第三方账号绑定多次
    UNIQUE(user_id, provider) -- 防止同一个用户绑定同一个平台多次
);

CREATE INDEX idx_social_users_user_id ON vx_social_users(user_id);
