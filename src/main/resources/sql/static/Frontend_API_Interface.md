# 前端接口服务文档

本文档描述了前端可以调用的主要接口服务，包括主页视频推荐、视频详情、上传和更新视频。

## 1. 主页视频推荐

### 1.1 获取热门视频 (Trending)
获取全站热门视频列表，无需登录即可访问。

- **URL**: `/api/recommend/trending`
- **Method**: `GET`
- **Parameters**:
    - `page` (int, optional): 页码，默认 1
    - `size` (int, optional): 每页数量，默认 10
- **Response**:
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 123,
            "title": "视频标题",
            "thumbnailUrl": "http://...",
            "duration": 120,
            "viewCount": 1000,
            "likeCount": 50,
            "uploaderNickname": "用户昵称",
            "uploaderAvatar": "http://...",
            "publishedAt": "2023-10-01T12:00:00"
        }
    ]
}
```

### 1.2 获取推荐视频流 (Feed)
获取个性化推荐视频。如果用户已登录，返回个性化推荐；如果未登录，返回随机/热门推荐。

- **URL**: `/api/recommend/feed`
- **Method**: `GET`
- **Parameters**:
    - `page` (int, optional): 页码，默认 1
    - `size` (int, optional): 每页数量，默认 10
- **Response**: 同上

---

## 2. 视频详情

### 2.1 获取视频详情
获取单个视频的完整信息，包括统计数据、上传者信息和当前用户的交互状态（是否点赞/收藏）。

- **URL**: `/api/videos/{id}`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <token>` (可选，用于获取交互状态)
- **Response**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 123,
        "title": "完整视频标题",
        "description": "视频描述...",
        "videoUrl": "http://.../video.mp4",
        "thumbnailUrl": "http://.../cover.jpg",
        "duration": 120,
        "category": "Technology",
        "tags": ["Java", "Spring"],
        "viewCount": 1005,
        "likeCount": 52,
        "uploaderId": 456,
        "uploaderNickname": "FlowBrain",
        "uploaderAvatar": "http://...",
        "isLiked": true,      // 当前用户是否点赞
        "isFavorited": false, // 当前用户是否收藏
        "publishedAt": "2023-10-01T12:00:00"
    }
}
```

---

## 3. 视频上传与更新

视频上传流程通常分为两步：
1. 上传视频文件（和封面图），获取文件URL。
2. 提交视频元数据（标题、描述、URL等）创建视频记录。

### 3.1 上传视频文件
- **URL**: `/api/videos/upload`
- **Method**: `POST`
- **Headers**: 
    - `Authorization: Bearer <token>`
    - `Content-Type: multipart/form-data`
- **Body**:
    - `file`: (Binary) 视频文件
- **Response**:
```json
{
    "code": 200,
    "message": "success",
    "data": "http://localhost/viewx/video_uuid.mp4" // 返回文件访问URL
}
```

### 3.2 上传封面图片
- **URL**: `/api/videos/upload/cover`
- **Method**: `POST`
- **Headers**: 同上
- **Body**:
    - `file`: (Binary) 图片文件
- **Response**:
```json
{
    "code": 200,
    "message": "success",
    "data": "http://localhost/viewx/cover_uuid.jpg"
}
```

### 3.3 创建/发布视频
提交视频信息进行发布。

- **URL**: `/api/videos`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
```json
{
    "title": "我的新视频",
    "description": "这是一个测试视频",
    "videoUrl": "http://localhost/viewx/video_uuid.mp4", // 3.1 接口返回的URL
    "thumbnailUrl": "http://localhost/viewx/cover_uuid.jpg", // 3.2 接口返回的URL
    "category": "Life",
    "tags": ["Vlog", "Daily"],
    "visibility": "PUBLIC" // PUBLIC, PRIVATE, UNLISTED
}
```
- **Response**:
```json
{
    "code": 200,
    "message": "success",
    "data": 789 // 新创建的视频ID
}
```

### 3.4 更新视频信息
更新已发布视频的信息。

- **URL**: `/api/videos/{id}`
- **Method**: `PUT`
- **Headers**: `Authorization: Bearer <token>`
- **Body**: (仅包含需要修改的字段)
```json
{
    "title": "修改后的标题",
    "description": "修改后的描述",
    "tags": ["NewTag"]
}
```
- **Response**:
```json
{
    "code": 200,
    "message": "视频更新成功",
    "data": "视频更新成功"
}
```

---

## 4. 交互操作 (补充)

### 4.1 点赞/取消点赞
- **URL**: `/api/interactions/like/{videoId}`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <token>`

### 4.2 收藏/取消收藏
- **URL**: `/api/interactions/favorite/{videoId}`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <token>`

### 4.3 获取交互状态
- **URL**: `/api/interactions/status/{videoId}`
- **Method**: `GET`
- **Response**: `{"liked": true, "favorited": false}`
