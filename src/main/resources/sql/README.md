# 数据库表结构说明

本目录包含 ViewX 项目的所有数据库表结构 SQL 文件。

## 📋 表结构文件列表

### 核心表
1. **users.sql** - 用户相关表
   - `vx_users` - 用户主表
   - `vx_user_details` - 用户详情表

2. **videos.sql** - 视频相关表
   - `vx_videos` - 视频主表
   - `vx_video_analytics` - 视频分析表

3. **interactions.sql** - 互动相关表
   - `vx_video_likes` - 视频点赞表
   - `vx_video_favorites` - 视频收藏表
   - `vx_video_comments` - 视频评论表

4. **user_follows.sql** - 用户关注关系表 ⭐ 新增
   - `vx_user_follows` - 用户关注关系表

### 扩展表
5. **oauth2.sql** - OAuth2 认证表
6. **logs_and_notifications.sql** - 日志和通知表
7. **ai_extension.sql** - AI 扩展表
8. **ai_models.sql** - AI 模型配置表

## 🚀 执行顺序

**重要**：必须按照以下顺序执行 SQL 文件，因为存在外键依赖关系。

```bash
# 1. 用户表（基础依赖）
psql -U your_username -d viewx_db -f users.sql

# 2. 视频表（依赖用户表）
psql -U your_username -d viewx_db -f videos.sql

# 3. 用户关注表（依赖用户表）⭐ 新增
psql -U your_username -d viewx_db -f user_follows.sql

# 4. 互动表（依赖用户表和视频表）
psql -U your_username -d viewx_db -f interactions.sql

# 5. OAuth2 表（依赖用户表）
psql -U your_username -d viewx_db -f oauth2.sql

# 6. 日志和通知表（依赖用户表）
psql -U your_username -d viewx_db -f logs_and_notifications.sql

# 7. AI 扩展表（依赖视频表）
psql -U your_username -d viewx_db -f ai_extension.sql

# 8. AI 模型配置表
psql -U your_username -d viewx_db -f ai_models.sql
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

如果您想一次性执行所有 SQL 文件：

```bash
#!/bin/bash
# init_database.sh

DB_USER="your_username"
DB_NAME="viewx_db"
SQL_DIR="src/main/resources/sql"

echo "开始初始化数据库..."

# 按顺序执行
psql -U $DB_USER -d $DB_NAME -f $SQL_DIR/users.sql
psql -U $DB_USER -d $DB_NAME -f $SQL_DIR/videos.sql
psql -U $DB_USER -d $DB_NAME -f $SQL_DIR/user_follows.sql
psql -U $DB_USER -d $DB_NAME -f $SQL_DIR/interactions.sql
psql -U $DB_USER -d $DB_NAME -f $SQL_DIR/oauth2.sql
psql -U $DB_USER -d $DB_NAME -f $SQL_DIR/logs_and_notifications.sql
psql -U $DB_USER -d $DB_NAME -f $SQL_DIR/ai_extension.sql
psql -U $DB_USER -d $DB_NAME -f $SQL_DIR/ai_models.sql

echo "数据库初始化完成！"
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
