# 数据库表结构说明

本目录包含 ViewX 项目的所有数据库表结构 SQL 文件。

## 📋 表结构文件列表

**说明**: 所有 SQL 文件已使用数字前缀命名,Docker 初始化时会按文件名字母顺序自动执行。

### 核心表
1. **01_users.sql** - 用户相关表
   - `vx_users` - 用户主表
   - `vx_user_details` - 用户详情表

2. **02_videos.sql** - 视频相关表
   - `vx_videos` - 视频主表 (已包含 cover_url 字段和可选的 category)
   - `vx_video_analytics` - 视频分析表

3. **03_contents.sql** - 内容相关表
   - `vx_contents` - 统一内容表 (视频/图片/图片集)

4. **04_topics.sql** - 话题相关表
   - `vx_topics` - 话题表
   - `vx_video_topics` - 视频-话题关联表

5. **05_user_follows.sql** - 用户关注关系表
   - `vx_user_follows` - 用户关注关系表

6. **06_interactions.sql** - 互动相关表
   - `vx_video_likes` - 视频点赞表
   - `vx_video_favorites` - 视频收藏表
   - `vx_video_comments` - 视频评论表

### 扩展表
7. **07_oauth2.sql** - OAuth2 认证表
8. **08_logs_and_notifications.sql** - 日志和通知表
9. **09_ai_extension.sql** - AI 扩展表
10. **10_ai_models.sql** - AI 模型配置表
11. **11_messages.sql** - 聊天消息表 ⭐ 新增
    - `vx_messages` - 私信消息表
    - `vx_conversations` - 会话表
    - `vx_user_online_status` - 用户在线状态表

## 🚀 执行顺序

**重要**：SQL 文件已使用数字前缀命名,Docker 会按字母顺序自动执行,无需手动指定顺序。

如果需要手动执行,请按以下顺序:

```bash
# 1. 用户表（基础依赖）
psql -U postgres -d viewx_db -f 01_users.sql

# 2. 视频表（依赖用户表）
psql -U postgres -d viewx_db -f 02_videos.sql

# 3. 内容表（依赖用户表）
psql -U postgres -d viewx_db -f 03_contents.sql

# 4. 话题表（依赖视频表）
psql -U postgres -d viewx_db -f 04_topics.sql

# 5. 用户关注表（依赖用户表）
psql -U postgres -d viewx_db -f 05_user_follows.sql

# 6. 互动表（依赖用户表和视频表）
psql -U postgres -d viewx_db -f 06_interactions.sql

# 7. OAuth2 表（依赖用户表）
psql -U postgres -d viewx_db -f 07_oauth2.sql

# 8. 日志和通知表（依赖用户表）
psql -U postgres -d viewx_db -f 08_logs_and_notifications.sql

# 9. AI 扩展表（依赖视频表和用户表）
psql -U postgres -d viewx_db -f 09_ai_extension.sql

# 10. AI 模型配置表
psql -U postgres -d viewx_db -f 10_ai_models.sql

# 11. 聊天消息表（依赖用户表）
psql -U postgres -d viewx_db -f 11_messages.sql
```

## 📊 表关系图

```
vx_users (用户表)
    ├── vx_user_details (1:1)
    ├── vx_user_follows (N:N 自关联) ⭐ 新增
    ├── vx_videos (1:N)
    ├── vx_video_likes (N:N)
    ├── vx_video_favorites (N:N)
    └── vx_video_comments (1:N)

vx_videos (视频表)
    ├── vx_video_analytics (1:1)
    ├── vx_video_likes (N:N)
    ├── vx_video_favorites (N:N)
    └── vx_video_comments (1:N)
```

## 🔧 快速初始化（一键执行）

**Docker 环境**：使用 `docker-compose up -d` 会自动按顺序执行所有 SQL 文件。

**手动初始化**：如果需要手动执行所有 SQL 文件：

```bash
#!/bin/bash
# init_database.sh

DB_USER="postgres"
DB_NAME="viewx_db"
SQL_DIR="src/main/resources/sql"

echo "开始初始化数据库..."

# 按数字前缀顺序自动执行
for sql_file in $SQL_DIR/*.sql; do
    echo "执行: $sql_file"
    psql -U $DB_USER -d $DB_NAME -f "$sql_file"
done

echo "数据库初始化完成！"
```

或者手动按顺序执行:

```bash
psql -U postgres -d viewx_db -f src/main/resources/sql/01_users.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/02_videos.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/03_contents.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/04_topics.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/05_user_follows.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/06_interactions.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/07_oauth2.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/08_logs_and_notifications.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/09_ai_extension.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/10_ai_models.sql
psql -U postgres -d viewx_db -f src/main/resources/sql/11_messages.sql
```

## ⚠️ 注意事项

1. **PostgreSQL 版本要求**：建议使用 PostgreSQL 14+ 版本。
2. **pgvector 扩展**：部分表使用了 `vector` 类型，需要先安装 pgvector 扩展：
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
3. **权限**：确保数据库用户有创建表、索引和扩展的权限。
4. **备份**：在生产环境执行前，请务必备份现有数据。

## 📝 新增表说明

### vx_user_follows（用户关注关系表）

**用途**：记录用户之间的关注关系，支持以下功能：
- 查询某用户的粉丝列表
- 查询某用户的关注列表
- 统计粉丝数和关注数
- 判断两个用户之间是否存在关注关系

**字段说明**：
- `follower_id`: 关注者用户 ID
- `followed_id`: 被关注者用户 ID
- `created_at`: 关注时间

**约束**：
- 主键：`(follower_id, followed_id)` - 防止重复关注
- 检查约束：`follower_id != followed_id` - 防止自己关注自己
- 外键：关联到 `vx_users` 表，级联删除

**索引**：
- `idx_follows_followed_time`: 优化查询粉丝列表
- `idx_follows_follower_time`: 优化查询关注列表

## 🔗 相关代码

- **Mapper**: `ProfileMapper.java`
- **Service**: `ProfileService.java`
- **VO**: `UserProfileVO.java`

这些代码使用了 `vx_user_follows` 表来统计用户的粉丝数和关注数。
