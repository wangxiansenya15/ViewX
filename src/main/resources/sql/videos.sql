-- 启用 pgvector 扩展（需在使用 VECTOR 类型前执行）
CREATE EXTENSION IF NOT EXISTS vector;


-- 视频主表
CREATE TABLE vx_videos (
                        id BIGINT PRIMARY KEY, -- 雪花算法ID

    -- 基础信息
                        title VARCHAR(200) NOT NULL,
                        description TEXT,
                        duration INTEGER NOT NULL CHECK (duration > 0),

    -- 媒体文件信息
                        video_url VARCHAR(500) NOT NULL,
                        cover_url VARCHAR(500), -- 封面图片URL
                        thumbnail_url VARCHAR(500),
                        preview_url VARCHAR(500), -- 预览片段
                        file_size BIGINT,
                        format VARCHAR(10),
                        resolution VARCHAR(20), -- 分辨率 1080p, 4K等


    -- 内容分类
                        category VARCHAR(50) DEFAULT '其他',
                        subcategory VARCHAR(50),
                        tags VARCHAR(100)[],

    -- 权限和状态
                        visibility VARCHAR(20) DEFAULT 'PUBLIC' CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'UNLISTED')),
                        status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'DELETED', 'PROCESSING')),

    -- 上传者信息
                        uploader_id BIGINT NOT NULL REFERENCES vx_users(id),

    -- 统计信息
                        view_count BIGINT DEFAULT 0,
                        like_count BIGINT DEFAULT 0,
                        dislike_count BIGINT DEFAULT 0,
                        share_count BIGINT DEFAULT 0,
                        comment_count BIGINT DEFAULT 0,
                        average_watch_time INTEGER, -- 平均观看时长(秒)

    -- AI分析字段
                        ai_tags JSONB, -- AI自动生成的标签 { "objects": [], "scenes": [], "topics": [] }
                        content_summary TEXT, -- AI生成的内容摘要
                        sentiment_score DECIMAL(3,2), -- 情感分析得分 -1.0 到 1.0
                        content_embedding VECTOR(512), -- 内容语义向量

    -- 时间戳
                        created_at TIMESTAMP DEFAULT NOW(),
                        updated_at TIMESTAMP DEFAULT NOW(),
                        published_at TIMESTAMP,
                        
    -- 软删除
                        is_deleted BOOLEAN DEFAULT FALSE,
                        deleted_at TIMESTAMP
);

CREATE INDEX idx_videos_uploader ON vx_videos(uploader_id);
CREATE INDEX idx_videos_category ON vx_videos(category);
CREATE INDEX idx_videos_created_at ON vx_videos(created_at DESC);
CREATE INDEX idx_videos_status ON vx_videos(status);
CREATE INDEX idx_videos_ai_tags ON vx_videos USING GIN (ai_tags);
CREATE INDEX idx_videos_embedding ON vx_videos USING ivfflat (content_embedding vector_cosine_ops);
CREATE INDEX idx_videos_trending ON vx_videos (created_at DESC, view_count DESC) WHERE status = 'APPROVED';

-- 视频分析表
CREATE TABLE vx_video_analytics (
                                 id BIGINT PRIMARY KEY, -- 雪花算法ID
                                 video_id BIGINT NOT NULL REFERENCES vx_videos(id) ON DELETE CASCADE,

    -- 内容特征
                                 visual_features VECTOR(1024), -- 视觉特征向量
                                 audio_features VECTOR(256), -- 音频特征向量

    -- 互动模式分析
                                 engagement_score DECIMAL(5,4) DEFAULT 0,
                                 completion_rate DECIMAL(5,4), -- 完播率
                                 retention_curve DECIMAL(5,4)[], -- 留存曲线数据

    -- 质量评估
                                 quality_score DECIMAL(3,2),
                                 content_safety_score DECIMAL(3,2),
                                 copyright_risk_score DECIMAL(3,2),

    -- 推荐相关
                                 trending_score DECIMAL(6,4), -- 热度评分
                                 personalization_bias JSONB, -- 个性化偏置向量

    -- 时间戳
                                 analyzed_at TIMESTAMPTZ DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ DEFAULT NOW(),

                                 UNIQUE(video_id)
);

CREATE INDEX idx_video_analytics_video_id ON vx_video_analytics(video_id);
CREATE INDEX idx_video_analytics_trending ON vx_video_analytics(trending_score DESC);