-- 启用 pgvector 扩展（需在使用 VECTOR 类型前执行）
CREATE EXTENSION IF NOT EXISTS vector;

-- 搜索历史表
CREATE TABLE vx_search_history (
                                id BIGINT PRIMARY KEY, -- 雪花算法ID
                                user_id BIGINT REFERENCES vx_users(id) ON DELETE SET NULL,

    -- 搜索内容
                                query_text TEXT NOT NULL,
                                search_filters JSONB DEFAULT '{}', -- 搜索筛选条件
                                search_type VARCHAR(20) DEFAULT 'TEXT' CHECK (search_type IN ('TEXT', 'VOICE', 'IMAGE', 'HYBRID')),

    -- 搜索结果
                                result_count INTEGER,
                                search_session_id BIGINT, -- 同一搜索会话的ID

    -- AI增强字段
                                intent_classification VARCHAR(50), -- AI分类的搜索意图
                                semantic_query_vector VECTOR(384), -- 语义搜索向量
                                query_expansion_terms VARCHAR(100)[], -- 查询扩展词

    -- 时间戳
                                searched_at TIMESTAMPTZ DEFAULT NOW()
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_search_history_user_id ON vx_search_history(user_id);
CREATE INDEX IF NOT EXISTS idx_search_history_searched_at ON vx_search_history(searched_at DESC);
CREATE INDEX IF NOT EXISTS idx_search_history_session ON vx_search_history(search_session_id);
CREATE INDEX IF NOT EXISTS idx_search_history_vector ON vx_search_history USING ivfflat (semantic_query_vector vector_cosine_ops);

-- 用户行为表
CREATE TABLE vx_user_behavior (
                                  id BIGINT PRIMARY KEY, -- 雪花算法ID
                                  user_id BIGINT NOT NULL REFERENCES vx_users(id),
                                  video_id BIGINT NOT NULL REFERENCES vx_videos(id),

    -- 行为类型
                                  behavior_type VARCHAR(20) NOT NULL CHECK (behavior_type IN (
                                                                                              'VIEW', 'LIKE', 'DISLIKE', 'SHARE', 'COMMENT', 'SAVE', 'COMPLETE', 'SKIP', 'CLICK', 'IMPRESSION'
                                      )),

    -- 行为详情
                                  duration_watched INTEGER, -- 观看时长(秒)
                                  progress_percentage DECIMAL(5,2), -- 观看进度百分比
                                  device_info JSONB,
                                  location_info JSONB,

    -- AI特征
                                  user_preference_vector VECTOR(300), -- 用户偏好向量
                                  context_vector VECTOR(200), -- 上下文向量(时间、位置等)

    -- 时间戳
                                  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_user_behavior_user_id ON vx_user_behavior(user_id);
CREATE INDEX IF NOT EXISTS idx_user_behavior_video_id ON vx_user_behavior(video_id);
CREATE INDEX IF NOT EXISTS idx_user_behavior_type ON vx_user_behavior(behavior_type);
CREATE INDEX IF NOT EXISTS idx_user_behavior_created_at ON vx_user_behavior(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_behavior_composite ON vx_user_behavior(user_id, video_id, behavior_type);

-- 推荐结果表
CREATE TABLE vx_recommendations (
                                 id BIGINT PRIMARY KEY, -- 雪花算法ID
                                 user_id BIGINT NOT NULL REFERENCES vx_users(id),
                                 video_id BIGINT NOT NULL REFERENCES vx_videos(id),

    -- 推荐信息
                                 recommendation_type VARCHAR(30) NOT NULL CHECK (recommendation_type IN (
                                                                                                         'CONTENT_BASED', 'COLLABORATIVE', 'TRENDING', 'PERSONALIZED', 'CONTEXTUAL', 'SIMILAR', 'POPULAR'
                                     )),
                                 recommendation_score DECIMAL(5,4) NOT NULL,
                                 ranking_position INTEGER, -- 在推荐列表中的位置

    -- 上下文信息
                                 context JSONB, -- 推荐时的上下文（时间、位置、设备、场景等）
                                 explanation TEXT, -- 推荐理由(可解释AI)

    -- 反馈
                                 was_clicked BOOLEAN DEFAULT FALSE,
                                 watch_time INTEGER, -- 实际观看时长
                                 feedback_score INTEGER CHECK (feedback_score >= -1 AND feedback_score <= 1),

    -- 时间戳
                                 recommended_at TIMESTAMP DEFAULT NOW(),
                                 feedback_at TIMESTAMP,
                                 recommended_date DATE DEFAULT CURRENT_DATE
);

-- 创建索引和约束
CREATE INDEX IF NOT EXISTS idx_recommendations_user_id ON vx_recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_video_id ON vx_recommendations(video_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_score ON vx_recommendations(recommendation_score DESC);
CREATE INDEX IF NOT EXISTS idx_recommendations_clicked ON vx_recommendations(was_clicked);
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_recommendations_daily ON recommendations(user_id, video_id, recommendation_type, DATE(recommended_at));

DROP INDEX IF EXISTS uq_recommendations_daily;

-- 创建基于日期的唯一索引
CREATE UNIQUE INDEX uq_recommendations_daily
    ON vx_recommendations(user_id, video_id, recommendation_type, recommended_date);

-- 为日期列创建索引
CREATE INDEX IF NOT EXISTS idx_recommendations_date ON vx_recommendations(recommended_date);