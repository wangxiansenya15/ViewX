# 文件存储结构优化 v2.0

## 📁 最终目录结构

```
storage/
├── images/                    # 📷 所有图片内容
│   ├── avatars/               # 用户头像
│   │   └── {userId}/
│   │       └── main.png       # 固定文件名，方便缓存
│   │
│   ├── backgrounds/           # 用户背景图
│   │   └── {userId}/
│   │       └── bg_{timestamp}.jpg
│   │
│   └── posts/                 # 📝 图文内容
│       └── {userId}/
│           └── {postId}/
│               ├── image_1.jpg
│               ├── image_2.jpg
│               └── image_3.jpg
│
└── videos/                    # 🎬 所有视频内容
    └── {userId}/
        └── {videoId}/
            ├── source.mp4     # 原始视频
            ├── cover.jpg      # 视频封面
            ├── thumb.jpg      # 视频缩略图
            └── segments/      # HLS切片（预留）
```

## ✨ 为什么这样设计？

### 1. **职责分离** 🎯
- **images/**: 所有静态图片（头像、背景、图文）
- **videos/**: 所有视频及相关文件

**优点**:
- CDN分流更方便（图片CDN vs 视频CDN）
- 备份策略不同（图片全量备份，视频增量备份）
- 权限控制更清晰

### 2. **支持图文内容** 📝

**场景**: 用户发布图文动态（类似微博、朋友圈）

```java
// 发布图文内容
Long postId = createPost(userId, "今天天气真好！");

// 上传多张图片
for (int i = 0; i < images.length; i++) {
    String path = FilePathUtil.generatePostImagePath(userId, postId, i+1, ".jpg");
    // → images/posts/123/456/image_1.jpg
    storageStrategy.storeFile(images[i], path);
}
```

**优点**:
- 一个图文内容的所有图片在同一目录
- 删除图文时，直接删除整个目录
- 支持多图（1-9张）

### 3. **头像放在images下的原因** 🤔

**旧方案**: `avatars/{userId}/main.png`  
**新方案**: `images/avatars/{userId}/main.png`

**理由**:
1. **语义清晰**: 头像本质上就是图片
2. **统一管理**: 所有图片资源在images下，便于管理
3. **CDN配置**: 可以统一配置 `images/*` 的缓存策略
4. **扩展性**: 未来可能有更多图片类型（勋章、表情包等）

## 📊 对比分析

### 方案对比

| 方案 | 结构 | 优点 | 缺点 |
|------|------|------|------|
| **旧方案** | `avatars/`, `images/`, `videos/` | 简单直观 | 职责不清，扩展性差 |
| **新方案** | `images/`, `videos/` | 职责清晰，支持图文 | 路径稍长 |

### 实际案例

#### 用户A的文件分布

```
storage/
├── images/
│   ├── avatars/123/main.png          # 用户A的头像
│   ├── backgrounds/123/bg_xxx.jpg    # 用户A的背景图
│   └── posts/
│       ├── 123/1001/                 # 用户A的图文1
│       │   ├── image_1.jpg
│       │   └── image_2.jpg
│       └── 123/1002/                 # 用户A的图文2
│           ├── image_1.jpg
│           ├── image_2.jpg
│           └── image_3.jpg
│
└── videos/
    └── 123/
        ├── 2001/                     # 用户A的视频1
        │   ├── source.mp4
        │   ├── cover.jpg
        │   └── thumb.jpg
        └── 2002/                     # 用户A的视频2
            ├── source.mp4
            ├── cover.jpg
            └── thumb.jpg
```

## 🔧 代码示例

### 1. 上传头像

```java
// 生成路径
String avatarPath = FilePathUtil.generateAvatarPath(userId, ".png");
// → images/avatars/123/main.png

// 上传文件
storageStrategy.storeFile(avatarFile, avatarPath);
String avatarUrl = storageStrategy.getFileUrl(avatarPath);
```

### 2. 发布图文内容

```java
// 1. 创建图文记录
Post post = new Post();
post.setUserId(userId);
post.setContent("今天去爬山了！");
postMapper.insert(post);
Long postId = post.getId();

// 2. 上传图片
List<String> imageUrls = new ArrayList<>();
for (int i = 0; i < imageFiles.length; i++) {
    String path = FilePathUtil.generatePostImagePath(userId, postId, i+1, ".jpg");
    storageStrategy.storeFile(imageFiles[i], path);
    imageUrls.add(storageStrategy.getFileUrl(path));
}

// 3. 更新图文记录
post.setImageUrls(imageUrls);
postMapper.updateById(post);
```

### 3. 上传视频

```java
// 1. 创建视频记录
Video video = new Video();
video.setUserId(userId);
videoMapper.insert(video);
Long videoId = video.getId();

// 2. 上传视频文件
String videoPath = FilePathUtil.generateVideoSourcePath(userId, videoId, ".mp4");
String coverPath = FilePathUtil.generateVideoCoverPath(userId, videoId, ".jpg");

storageStrategy.storeFile(videoFile, videoPath);
storageStrategy.storeFile(coverFile, coverPath);
```

## 🚀 迁移建议

### 渐进式迁移

1. **新上传使用新结构**
   - 头像: `images/avatars/{userId}/main.png`
   - 视频: `videos/{userId}/{videoId}/source.mp4`

2. **旧文件保持不变**
   - 数据库存储的是完整URL，不受影响
   - 旧文件可以逐步迁移或自然淘汰

3. **兼容性处理**
   ```java
   // 同时支持新旧路径
   if (avatarUrl.contains("avatars/")) {
       // 旧路径: avatars/123/main.png
   } else if (avatarUrl.contains("images/avatars/")) {
       // 新路径: images/avatars/123/main.png
   }
   ```

## 📈 性能优化

### CDN配置示例

```nginx
# 图片CDN配置
location /images/ {
    # 长期缓存（头像、背景图不常变）
    expires 30d;
    add_header Cache-Control "public, immutable";
}

# 视频CDN配置
location /videos/ {
    # 中期缓存
    expires 7d;
    add_header Cache-Control "public";
}
```

### 存储策略

| 类型 | 存储位置 | 备份策略 | CDN |
|------|---------|---------|-----|
| 头像 | `images/avatars/` | 每日全量 | 图片CDN |
| 背景图 | `images/backgrounds/` | 每日全量 | 图片CDN |
| 图文图片 | `images/posts/` | 每日增量 | 图片CDN |
| 视频源文件 | `videos/*/source.mp4` | 每周增量 | 视频CDN |
| 视频封面 | `videos/*/cover.jpg` | 每日增量 | 图片CDN |

## ✅ 总结

### 新结构的优势

1. ✅ **职责清晰**: 图片和视频分离
2. ✅ **支持图文**: 新增posts目录
3. ✅ **易于管理**: 按用户和内容ID分层
4. ✅ **CDN友好**: 可以针对不同类型配置不同策略
5. ✅ **扩展性强**: 未来可以轻松添加新类型

### 适用场景

- ✅ 短视频平台（如TikTok、抖音）
- ✅ 图文社交平台（如微博、朋友圈）
- ✅ 混合内容平台（图文+视频）

---

**版本**: v2.0  
**更新时间**: 2025-12-24  
**作者**: ViewX Team
