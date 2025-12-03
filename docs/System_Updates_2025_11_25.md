# 🚀 ViewX 系统架构与功能更新日志

本文档记录了项目在视频推荐、用户互动及数据库优化方面的最新进展与技术实现细节。

---

## 📅 2025-11-25 更新汇总

### 1. 🎬 视频推荐系统 (Video Recommendation)

实现了基于热度和个性化的混合推荐引擎。

#### 1.1 核心算法 (Newton's Law of Cooling Variant)
采用牛顿冷却定律变体算法，平衡视频的“热度”与“新鲜度”。

$$ Score = (Play \times 0.4 + Like \times 0.3 + Comment \times 0.2) \times e^{-0.1 \times HoursSincePublish} $$

*   **热度因子**：播放量、点赞数、评论数加权求和。
*   **衰减因子**：随着发布时间推移，分数呈指数级衰减，保证榜单流动性。

#### 1.2 架构实现
*   **Service**: `RecommendServiceImpl`
    *   **缓存策略**: 使用 Redis `ZSet` (`viewx:video:trending`) 存储实时热度榜，支持 $O(\log N)$ 复杂度的 TopN 查询。
    *   **降级策略**: Redis 穿透或未命中时，自动回退查询数据库 (`VideoMapper.selectLatestVideos`)。
*   **Controller**: `RecommendController`
    *   `GET /recommend/trending`: 获取全站热榜（公开）。
    *   `GET /recommend/feed`: 获取个性化推荐流（登录用户基于画像，游客基于热度）。

---

### 2. 💬 用户互动系统 (User Interactions)

构建了完整的社交互动闭环，包括点赞、收藏和评论功能。

#### 2.1 数据库设计 (`interactions.sql`)
新增了三张核心表，用于存储海量互动数据：

1.  **`vx_video_likes`** (点赞表)
    *   联合主键 `(user_id, video_id)` 防止重复点赞。
2.  **`vx_video_favorites`** (收藏表)
    *   记录用户收藏行为。
3.  **`vx_video_comments`** (评论表)
    *   支持无限级盖楼（通过 `parent_id` 字段）。

#### 2.2 核心逻辑 (`InteractionService` & `CommentService`)
*   **原子性操作**: 点赞/收藏时，使用事务同时更新关联表和视频主表的统计计数 (`like_count`)。
*   **推荐联动**: 每次用户互动（点赞/评论）都会异步触发 `RecommendService.updateVideoScore`，实时提升视频热度。

#### 2.3 API 接口
*   `POST /interactions/like/{videoId}`: 点赞/取消。
*   `POST /interactions/favorite/{videoId}`: 收藏/取消。
*   `GET /interactions/status/{videoId}`: 查询当前用户互动状态。
*   `POST /comments/{videoId}`: 发表评论/回复。
*   `GET /comments/{videoId}`: 获取评论列表。

---

### 3. ⚡ 数据库性能优化 (Database Optimization)

针对高并发场景下的“大V”用户和“爆款”视频查询瓶颈，进行了索引重构。

#### 3.1 痛点分析
*   **原设计**: 仅有主键索引。
*   **瓶颈**: 查询“某用户最近点赞的视频”或“按时间倒序展示评论”时，数据库需要全表扫描并进行内存排序 (`Using filesort`)，性能极差。

#### 3.2 优化方案 (Composite Indexes)
引入了覆盖查询和排序的复合索引：

| 表名 | 索引名称 | 索引字段 | 优化场景 | 预期提升 |
| :--- | :--- | :--- | :--- | :--- |
| `vx_video_comments` | `idx_comments_user` | `(user_id, created_at DESC)` | 查询某用户的所有评论 | **1000x** |
| `vx_video_comments` | `idx_comments_video_time` | `(video_id, created_at DESC)` | 加载视频评论区（按时间倒序） | **100x** |
| `vx_video_likes` | `idx_likes_user_time` | `(user_id, created_at DESC)` | 查询“我点赞的视频”列表 | **100x** |
| `vx_video_favorites` | `idx_favorites_user_time` | `(user_id, created_at DESC)` | 查询“我收藏的视频”列表 | **100x** |

---

### 4. 🛠️ 待办重构建议 (Refactoring Plan)

针对 `AuthenticationService` 提出的改进路线图：

*   **无状态化**: 移除 `HttpSession`，改用 Redis 存储验证码 (`auth:code:{email}`)。
*   **参数校验**: 引入 Spring Validation (`@Valid`)，将参数清洗逻辑从 Service 层剥离。
*   **职责拆分**: 将 `AuthenticationService` 拆分为 `AuthService` (登录/Token) 和 `AccountService` (账号管理)。
*   **异常处理**: 统一使用全局异常处理器 (`GlobalExceptionHandler`)。

---

> 文档生成时间: 2025-11-25
