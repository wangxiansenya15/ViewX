-- 登录审计表（记录所有登录尝试）
CREATE TABLE vx_login_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT, -- 可以为空（登录失败时可能没有user_id）
    username VARCHAR(50) NOT NULL,
    
    -- 登录结果
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(200),
    
    -- 客户端信息
    ip_address VARCHAR(50) NOT NULL,
    user_agent VARCHAR(500),
    
    -- 风险评估
    risk_level VARCHAR(20), -- LOW, MEDIUM, HIGH
    
    -- 人机验证
    captcha_required BOOLEAN,
    captcha_verified BOOLEAN,
    
    -- 时间戳
    login_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 注释：索引已移至 13_indexes_optimization.sql 统一管理

-- 安全事件表（记录异常行为）
CREATE TABLE vx_security_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL, -- MULTIPLE_LOGIN_FAILURES, CAPTCHA_FAILURE, SUSPICIOUS_UA, etc.
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    
    -- 相关信息
    username VARCHAR(50),
    ip_address VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    
    -- 处理状态
    handled BOOLEAN DEFAULT FALSE,
    
    -- 时间戳
    event_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 注释：索引已移至 13_indexes_optimization.sql 统一管理
