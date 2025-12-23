
 -- 用户主表
CREATE TABLE vx_users (
                       id BIGINT PRIMARY KEY, -- 雪花算法ID
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_encrypted VARCHAR(255) NOT NULL,
                       phone VARCHAR(20),
                       nickname VARCHAR(50),

    -- 安全状态字段
                       enabled BOOLEAN DEFAULT TRUE,
                       account_non_expired BOOLEAN DEFAULT TRUE,
                       account_non_locked BOOLEAN DEFAULT TRUE,
                       credentials_non_expired BOOLEAN DEFAULT TRUE,

    -- 角色和权限
                       role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'USER', 'CONTENT_CREATOR','REVIEWER')),

    -- 时间戳
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW(),
                       last_login_at TIMESTAMP,

    -- 软删除 (逻辑删除)
                       is_deleted BOOLEAN DEFAULT FALSE,
                       deleted_at TIMESTAMP,

                       CONSTRAINT valid_username CHECK (username ~ '^[a-zA-Z0-9_]{3,50}$'),
                       CONSTRAINT valid_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- 注释：索引已移至 13_indexes_optimization.sql 统一管理
-- 请在创建表后执行该文件以创建所有优化索引


-- 用户详情表
CREATE TABLE vx_user_details (
                              id BIGSERIAL PRIMARY KEY, -- 雪花算法ID
                              user_id BIGINT NOT NULL REFERENCES vx_users(id) ON DELETE CASCADE,

    -- 个人信息
                              avatar_url VARCHAR(255),
                              description TEXT,
                              age INTEGER CHECK (age >= 0 AND age <= 150),
                              gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY')),

    -- 地理位置
                              country VARCHAR(50),
                              city VARCHAR(50),
                              address TEXT,

    -- 社交媒体
                              website_url VARCHAR(255),
                              social_links JSONB,

    -- 偏好设置 (用于AI推荐)
                              preferred_categories VARCHAR(50)[],
                              content_preferences JSONB DEFAULT '{}',
                              watch_history_settings JSONB DEFAULT '{}',

    -- 时间戳
                              created_at TIMESTAMP DEFAULT NOW(),
                              updated_at TIMESTAMP DEFAULT NOW(),

    -- 索引
                              UNIQUE(user_id)
);

-- 注释：索引已移至 13_indexes_optimization.sql 统一管理
