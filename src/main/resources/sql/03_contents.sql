-- 内容表 - 支持视频、图片、图片集等多种内容类型
-- 这是对 vx_videos 表的扩展,支持更丰富的内容形式

CREATE TABLE vx_contents (
    id BIGINT PRIMARY KEY, -- 雪花算法ID

    -- 内容类型
    content_type VARCHAR(20) NOT NULL CHECK (content_type IN ('VIDEO', 'IMAGE', 'IMAGE_SET', 'ARTICLE')),

    -- 基础信息
    title VARCHAR(200) NOT NULL,
    description TEXT,

    -- 媒体文件信息
    primary_url VARCHAR(500) NOT NULL, -- 主要媒体URL (视频URL或主图片URL)
    cover_url VARCHAR(500),            -- 封面图片URL
    thumbnail_url VARCHAR(500),        -- 缩略图URL
    media_urls VARCHAR(500)[],         -- 多媒体URL列表 (用于图片集)

    -- 视频专用字段
    duration INTEGER,                  -- 视频时长（秒），图片内容为null
    preview_url VARCHAR(500),          -- 预览片段
    file_size BIGINT,
    format VARCHAR(10),
    resolution VARCHAR(20),            -- 分辨率 1080p, 4K等

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

    -- AI分析字段
    ai_tags JSONB,                     -- AI自动生成的标签
    content_summary TEXT,              -- AI生成的内容摘要
    sentiment_score DECIMAL(3,2),      -- 情感分析得分 -1.0 到 1.0
    content_embedding VECTOR(512),     -- 内容语义向量

    -- 时间戳
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    published_at TIMESTAMP,
    
    -- 软删除
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- 注释：索引已移至 13_indexes_optimization.sql 统一管理
