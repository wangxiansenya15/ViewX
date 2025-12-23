-- ========================================
-- 统一索引优化脚本
-- ========================================
-- 
-- 说明：
-- 1. 本文件整合了所有表的索引（原有索引 + 优化索引）
-- 2. 使用 IF NOT EXISTS 避免重复创建
-- 3. 原表索引已全部启用，不再是注释
-- 4. 新增了 50+ 个性能优化索引
-- 
-- IDE 警告说明：
-- - 'gin_trgm_ops' 无法解析：这是 pg_trgm 扩展的操作符类，IDE 不认识但 PostgreSQL 支持
-- - 'relname' 等系统视图列无法解析：这是 PostgreSQL 系统视图的列，IDE 不认识但运行时有效
-- - 这些警告可以忽略，不影响脚本执行
-- 
-- 执行方式：
-- psql -U postgres -d viewx_db -f src/main/resources/sql/13_indexes_optimization.sql
-- 
-- 或者在 psql 中：
-- \i src/main/resources/sql/13_indexes_optimization.sql
-- 
-- ========================================

-- 0. 准备工作
-- ========================================

-- 注意：以下扩展安装可能需要超级用户权限
-- 如果遇到权限问题，请使用超级用户执行：
-- psql -U postgres -d viewx_db -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"

-- 启用 pg_trgm 扩展（用于模糊搜索）
-- IDE 可能会提示 'gin_trgm_ops' 无法解析，这是正常的，运行时会正确识别
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
    RAISE NOTICE 'pg_trgm extension is ready';
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE WARNING 'Insufficient privilege to create pg_trgm extension. Please run as superuser.';
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to create pg_trgm extension: %', SQLERRM;
END
$$;

-- 启用 vector 扩展（用于向量搜索）
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
    RAISE NOTICE 'vector extension is ready';
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE WARNING 'Insufficient privilege to create vector extension. Please run as superuser.';
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to create vector extension: %', SQLERRM;
END
$$;

-- ========================================
-- 1. 用户表 (vx_users) 索引优化
-- ========================================

-- 原表索引（保留并优化）：

-- 1.1 删除冗余的单列索引（UNIQUE 约束已创建索引）
DROP INDEX IF EXISTS idx_users_username;
DROP INDEX IF EXISTS idx_users_email;

-- 1.2 保留原有的其他索引
CREATE INDEX IF NOT EXISTS idx_users_created_at ON vx_users(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_role ON vx_users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON vx_users(enabled, account_non_expired, account_non_locked, credentials_non_expired);

-- 新增优化索引：

-- 1.3 优化用户名查询（部分索引，只索引未删除用户）
CREATE INDEX IF NOT EXISTS idx_users_username_active ON vx_users(username) 
WHERE is_deleted = FALSE;

-- 1.4 优化邮箱查询（部分索引）
CREATE INDEX IF NOT EXISTS idx_users_email_active ON vx_users(email) 
WHERE is_deleted = FALSE;

-- 1.5 优化登录查询（复合索引）
CREATE INDEX IF NOT EXISTS idx_users_login ON vx_users(username, enabled, account_non_locked) 
WHERE is_deleted = FALSE;

-- 1.6 优化邮箱登录查询
CREATE INDEX IF NOT EXISTS idx_users_email_login ON vx_users(email, enabled, account_non_locked) 
WHERE is_deleted = FALSE;

-- 1.7 优化用户名搜索（GIN 索引，支持模糊搜索）
CREATE INDEX IF NOT EXISTS idx_users_username_gin ON vx_users USING gin(username gin_trgm_ops);

-- 1.8 优化昵称搜索
CREATE INDEX IF NOT EXISTS idx_users_nickname_gin ON vx_users USING gin(nickname gin_trgm_ops);

-- 1.9 优化活跃用户查询
CREATE INDEX IF NOT EXISTS idx_users_created_at_active ON vx_users(created_at DESC) 
WHERE is_deleted = FALSE;

-- 1.10 优化角色和状态组合查询
CREATE INDEX IF NOT EXISTS idx_users_role_status ON vx_users(role, enabled) 
WHERE is_deleted = FALSE;

-- 1.11 优化最后登录时间查询
CREATE INDEX IF NOT EXISTS idx_users_last_login ON vx_users(last_login_at DESC NULLS LAST) 
WHERE is_deleted = FALSE;

-- ========================================
-- 2. 视频表 (vx_videos) 索引优化
-- ========================================

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_videos_uploader ON vx_videos(uploader_id);
CREATE INDEX IF NOT EXISTS idx_videos_category ON vx_videos(category);
CREATE INDEX IF NOT EXISTS idx_videos_created_at ON vx_videos(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_videos_status ON vx_videos(status);
CREATE INDEX IF NOT EXISTS idx_videos_ai_tags ON vx_videos USING GIN (ai_tags);
CREATE INDEX IF NOT EXISTS idx_videos_embedding ON vx_videos USING ivfflat (content_embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_videos_trending ON vx_videos (created_at DESC, view_count DESC) WHERE status = 'APPROVED';

-- 新增优化索引：

-- 2.1 优化视频列表查询（按状态和时间）
CREATE INDEX IF NOT EXISTS idx_videos_status_time ON vx_videos(status, created_at DESC) 
WHERE is_deleted = FALSE;

-- 2.2 优化用户视频查询
CREATE INDEX IF NOT EXISTS idx_videos_uploader_time ON vx_videos(uploader_id, created_at DESC) 
WHERE is_deleted = FALSE;

-- 2.3 优化分类浏览
CREATE INDEX IF NOT EXISTS idx_videos_category_status_time ON vx_videos(category, status, created_at DESC) 
WHERE is_deleted = FALSE AND status = 'APPROVED';

-- 2.4 优化热门视频查询
CREATE INDEX IF NOT EXISTS idx_videos_hot ON vx_videos(view_count DESC, like_count DESC, created_at DESC) 
WHERE is_deleted = FALSE AND status = 'APPROVED';

-- 2.5 优化标签搜索
CREATE INDEX IF NOT EXISTS idx_videos_tags_gin ON vx_videos USING GIN(tags);

-- 2.6 优化视频所有者查询（用于权限检查）
CREATE INDEX IF NOT EXISTS idx_videos_id_uploader ON vx_videos(id, uploader_id) 
WHERE is_deleted = FALSE;

-- 2.7 优化标题搜索
CREATE INDEX IF NOT EXISTS idx_videos_title_trgm ON vx_videos USING gin(title gin_trgm_ops);

-- 2.8 优化描述搜索
CREATE INDEX IF NOT EXISTS idx_videos_description_trgm ON vx_videos USING gin(description gin_trgm_ops);

-- ========================================
-- 3. 内容表 (vx_contents) 索引优化
-- ========================================

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_contents_uploader ON vx_contents(uploader_id);
CREATE INDEX IF NOT EXISTS idx_contents_type ON vx_contents(content_type);
CREATE INDEX IF NOT EXISTS idx_contents_category ON vx_contents(category);
CREATE INDEX IF NOT EXISTS idx_contents_created_at ON vx_contents(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_contents_status ON vx_contents(status);
CREATE INDEX IF NOT EXISTS idx_contents_ai_tags ON vx_contents USING GIN (ai_tags);
CREATE INDEX IF NOT EXISTS idx_contents_embedding ON vx_contents USING ivfflat (content_embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_contents_trending ON vx_contents (created_at DESC, view_count DESC) WHERE status = 'APPROVED';

-- 新增优化索引：

-- 3.1 优化内容类型和状态查询
CREATE INDEX IF NOT EXISTS idx_contents_type_status_time ON vx_contents(content_type, status, created_at DESC) 
WHERE is_deleted = FALSE;

-- 3.2 优化用户内容查询
CREATE INDEX IF NOT EXISTS idx_contents_uploader_type_time ON vx_contents(uploader_id, content_type, created_at DESC) 
WHERE is_deleted = FALSE;

-- 3.3 优化可见性查询
CREATE INDEX IF NOT EXISTS idx_contents_visibility_status ON vx_contents(visibility, status, created_at DESC) 
WHERE is_deleted = FALSE;

-- 3.4 优化标签搜索
CREATE INDEX IF NOT EXISTS idx_contents_tags_gin ON vx_contents USING GIN(tags);

-- 3.5 优化标题搜索
CREATE INDEX IF NOT EXISTS idx_contents_title_trgm ON vx_contents USING gin(title gin_trgm_ops);

-- ========================================
-- 4. 互动表索引优化
-- ========================================

-- 4.1 点赞表 (vx_video_likes)

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_likes_user_time ON vx_video_likes(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_likes_video ON vx_video_likes(video_id);

-- 新增优化索引：
CREATE INDEX IF NOT EXISTS idx_video_likes_check ON vx_video_likes(video_id, user_id);

-- 4.2 收藏表 (vx_video_favorites)

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_favorites_user_time ON vx_video_favorites(user_id, created_at DESC);

-- 新增优化索引：
CREATE INDEX IF NOT EXISTS idx_video_favorites_check ON vx_video_favorites(video_id, user_id);

-- 4.3 评论表 (vx_video_comments)

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_comments_video_time ON vx_video_comments(video_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_parent ON vx_video_comments(parent_id);
CREATE INDEX IF NOT EXISTS idx_comments_user ON vx_video_comments(user_id, created_at DESC);

-- 新增优化索引：

-- 优化视频评论列表查询（过滤已删除）
CREATE INDEX IF NOT EXISTS idx_comments_video_deleted ON vx_video_comments(video_id, created_at DESC) 
WHERE is_deleted = FALSE;

-- 优化用户评论历史查询
CREATE INDEX IF NOT EXISTS idx_comments_user_deleted ON vx_video_comments(user_id, created_at DESC) 
WHERE is_deleted = FALSE;

-- 优化热门评论查询
CREATE INDEX IF NOT EXISTS idx_comments_hot ON vx_video_comments(video_id, like_count DESC, created_at DESC) 
WHERE is_deleted = FALSE AND is_pinned = FALSE;

-- 优化置顶评论查询
CREATE INDEX IF NOT EXISTS idx_comments_pinned ON vx_video_comments(video_id, is_pinned, created_at DESC) 
WHERE is_deleted = FALSE AND is_pinned = TRUE;

-- ========================================
-- 5. 消息表 (vx_messages) 索引优化
-- ========================================

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_messages_sender ON vx_messages(sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON vx_messages(receiver_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON vx_messages(sender_id, receiver_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_unread ON vx_messages(receiver_id, is_read) WHERE is_read = FALSE;
CREATE INDEX IF NOT EXISTS idx_messages_is_deleted ON vx_messages(is_deleted);
CREATE INDEX IF NOT EXISTS idx_messages_is_recalled ON vx_messages(is_recalled);

-- 新增优化索引：

-- 5.1 优化会话消息查询（双向，使用函数索引）
CREATE INDEX IF NOT EXISTS idx_messages_conversation_both ON vx_messages(
    LEAST(sender_id, receiver_id), 
    GREATEST(sender_id, receiver_id), 
    created_at DESC
) WHERE is_deleted = FALSE;

-- 5.2 优化未读消息统计
CREATE INDEX IF NOT EXISTS idx_messages_unread_count ON vx_messages(receiver_id, is_read, created_at DESC) 
WHERE is_deleted = FALSE AND is_read = FALSE;

-- 5.3 优化已撤回消息过滤
CREATE INDEX IF NOT EXISTS idx_messages_not_recalled ON vx_messages(sender_id, receiver_id, created_at DESC) 
WHERE is_deleted = FALSE AND is_recalled = FALSE;

-- ========================================
-- 6. 会话表 (vx_conversations) 索引优化
-- ========================================

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_conversations_user1 ON vx_conversations(user1_id, last_message_time DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_user2 ON vx_conversations(user2_id, last_message_time DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_last_message ON vx_conversations(last_message_time DESC);

-- 新增优化索引：

-- 6.1 优化用户会话列表查询（user1，有未读消息）
CREATE INDEX IF NOT EXISTS idx_conversations_user1_unread ON vx_conversations(user1_id, last_message_time DESC) 
WHERE unread_count_user1 > 0;

-- 6.2 优化用户会话列表查询（user2，有未读消息）
CREATE INDEX IF NOT EXISTS idx_conversations_user2_unread ON vx_conversations(user2_id, last_message_time DESC) 
WHERE unread_count_user2 > 0;

-- ========================================
-- 7. 关注表 (vx_user_follows) 索引优化
-- ========================================

-- 新增索引：

-- 7.1 优化"检查是否已关注"查询
CREATE INDEX IF NOT EXISTS idx_follows_check ON vx_user_follows(follower_id, followed_id);

-- 7.2 优化粉丝列表查询
CREATE INDEX IF NOT EXISTS idx_follows_followers ON vx_user_follows(followed_id, created_at DESC);

-- 7.3 优化关注列表查询
CREATE INDEX IF NOT EXISTS idx_follows_following ON vx_user_follows(follower_id, created_at DESC);

-- ========================================
-- 8. 话题表 (vx_topics) 索引优化
-- ========================================

-- 新增索引：

-- 8.1 优化话题名称查询（唯一索引，不区分大小写）
CREATE UNIQUE INDEX IF NOT EXISTS idx_topics_name_unique ON vx_topics(LOWER(name)) 
WHERE is_deleted = FALSE;

-- 8.2 优化热门话题查询
CREATE INDEX IF NOT EXISTS idx_topics_hot ON vx_topics(video_count DESC, created_at DESC) 
WHERE is_deleted = FALSE;

-- ========================================
-- 9. 视频话题关联表 (vx_video_topics) 索引优化
-- ========================================

-- 新增索引：

-- 9.1 优化话题视频列表查询
CREATE INDEX IF NOT EXISTS idx_video_topics_topic_time ON vx_video_topics(topic_id, created_at DESC);

-- 9.2 优化视频话题列表查询（如果原表没有）
CREATE INDEX IF NOT EXISTS idx_video_topics_video ON vx_video_topics(video_id);

-- ========================================
-- 10. 通知表 (vx_notifications) 索引优化
-- ========================================

-- 新增索引：

-- 10.1 优化未读通知查询
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON vx_notifications(recipient_id, is_read, created_at DESC) 
WHERE is_read = FALSE;

-- 10.2 优化通知类型查询
CREATE INDEX IF NOT EXISTS idx_notifications_type ON vx_notifications(recipient_id, notification_type, created_at DESC);

-- 10.3 优化相关视频通知查询
CREATE INDEX IF NOT EXISTS idx_notifications_video ON vx_notifications(related_video_id, created_at DESC) 
WHERE related_video_id IS NOT NULL;

-- ========================================
-- 11. 安全审计表索引优化
-- ========================================

-- 11.1 登录审计表 (vx_login_audit)

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_login_audit_user_id ON vx_login_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_login_audit_username ON vx_login_audit(username);
CREATE INDEX IF NOT EXISTS idx_login_audit_ip ON vx_login_audit(ip_address);
CREATE INDEX IF NOT EXISTS idx_login_audit_time ON vx_login_audit(login_time DESC);
CREATE INDEX IF NOT EXISTS idx_login_audit_success ON vx_login_audit(success);

-- 新增优化索引：
CREATE INDEX IF NOT EXISTS idx_login_audit_user_time ON vx_login_audit(user_id, login_time DESC);
CREATE INDEX IF NOT EXISTS idx_login_audit_ip_time ON vx_login_audit(ip_address, login_time DESC);
CREATE INDEX IF NOT EXISTS idx_login_audit_failed ON vx_login_audit(username, login_time DESC) 
WHERE success = FALSE;

-- 11.2 安全事件表 (vx_security_events)

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_security_events_type ON vx_security_events(event_type);
CREATE INDEX IF NOT EXISTS idx_security_events_severity ON vx_security_events(severity);
CREATE INDEX IF NOT EXISTS idx_security_events_ip ON vx_security_events(ip_address);
CREATE INDEX IF NOT EXISTS idx_security_events_time ON vx_security_events(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_security_events_handled ON vx_security_events(handled);

-- 新增优化索引：
CREATE INDEX IF NOT EXISTS idx_security_events_unhandled ON vx_security_events(severity DESC, event_time DESC) 
WHERE handled = FALSE;

CREATE INDEX IF NOT EXISTS idx_security_events_critical ON vx_security_events(event_time DESC) 
WHERE severity IN ('HIGH', 'CRITICAL');

-- ========================================
-- 12. 在线状态表 (vx_user_online_status) 索引优化
-- ========================================

-- 原表索引（保留）：
CREATE INDEX IF NOT EXISTS idx_online_status ON vx_user_online_status(is_online, last_seen DESC);

-- 新增优化索引：
CREATE INDEX IF NOT EXISTS idx_online_users ON vx_user_online_status(last_seen DESC) 
WHERE is_online = TRUE;

-- ========================================
-- 13. 统计信息更新
-- ========================================

ANALYZE vx_users;
ANALYZE vx_user_details;
ANALYZE vx_videos;
ANALYZE vx_video_analytics;
ANALYZE vx_contents;
ANALYZE vx_video_likes;
ANALYZE vx_video_favorites;
ANALYZE vx_video_comments;
ANALYZE vx_messages;
ANALYZE vx_conversations;
ANALYZE vx_user_online_status;
ANALYZE vx_user_follows;
ANALYZE vx_topics;
ANALYZE vx_video_topics;
ANALYZE vx_notifications;
ANALYZE vx_login_audit;
ANALYZE vx_security_events;

-- ========================================
-- 14. 查看索引使用情况
-- ========================================

-- 注意：以下查询使用 PostgreSQL 系统视图
-- 不同版本的 PostgreSQL 可能有不同的列名

-- 查看所有索引及其大小
SELECT 
    schemaname,
    relname AS tablename,
    indexrelname AS indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;

-- 查看未使用的索引（可能需要删除以节省空间）
SELECT 
    schemaname,
    relname AS tablename,
    indexrelname AS indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;

-- ========================================
-- 完成
-- ========================================
SELECT 'All indexes created successfully!' AS status;
