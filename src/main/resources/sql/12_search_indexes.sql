-- 用户搜索优化索引
-- 为用户名和昵称创建索引，支持快速模糊搜索

-- 1. 用户名索引（支持前缀匹配和精确匹配）
CREATE INDEX IF NOT EXISTS idx_users_username_search 
ON vx_users (username) 
WHERE is_deleted = false;

-- 2. 昵称索引（支持前缀匹配）
CREATE INDEX IF NOT EXISTS idx_users_nickname_search 
ON vx_users (nickname) 
WHERE is_deleted = false;

-- 3. 用户名模糊搜索索引（使用 pg_trgm 扩展，支持更好的模糊搜索）
-- 需要先启用 pg_trgm 扩展
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 创建 GIN 索引用于模糊搜索
CREATE INDEX IF NOT EXISTS idx_users_username_trgm 
ON vx_users USING gin (username gin_trgm_ops) 
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_users_nickname_trgm 
ON vx_users USING gin (nickname gin_trgm_ops) 
WHERE is_deleted = false;

-- 4. 视频标题搜索索引
CREATE INDEX IF NOT EXISTS idx_videos_title_search 
ON vx_videos (title) 
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_videos_title_trgm 
ON vx_videos USING gin (title gin_trgm_ops) 
WHERE is_deleted = false;

-- 5. 视频描述搜索索引（可选，如果需要搜索描述）
CREATE INDEX IF NOT EXISTS idx_videos_description_trgm 
ON vx_videos USING gin (description gin_trgm_ops) 
WHERE is_deleted = false;

-- 6. 复合索引：用户名 + 创建时间（用于排序）
CREATE INDEX IF NOT EXISTS idx_users_username_created 
ON vx_users (username, created_at DESC) 
WHERE is_deleted = false;

-- 7. 复合索引：昵称 + 创建时间（用于排序）
CREATE INDEX IF NOT EXISTS idx_users_nickname_created 
ON vx_users (nickname, created_at DESC) 
WHERE is_deleted = false;

-- 验证索引创建
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename IN ('vx_users', 'vx_videos')
  AND indexname LIKE '%search%' OR indexname LIKE '%trgm%'
ORDER BY tablename, indexname;
