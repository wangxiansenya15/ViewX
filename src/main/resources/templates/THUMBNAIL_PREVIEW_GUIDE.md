# 缩略图和预览图处理方案

## 当前字段说明

| 字段 | 用途 | 推荐尺寸 | 生成方式 |
|------|------|----------|----------|
| `cover_url` | 封面图（详情页展示） | 1280x720 | 用户上传 |
| `thumbnail_url` | 缩略图（列表展示） | 320x180 | 自动生成 |
| `preview_url` | 预览片段（悬停播放） | 480x270 | 自动生成（可选） |

---

## 实现方案

### 阶段一：基础版（当前）

**只使用封面图**：
- 用户上传封面 → `cover_url`
- 前端列表展示时直接使用 `cover_url`（浏览器会自动缩放）
- `thumbnail_url` 和 `preview_url` 暂时留空

**优点**：
- 实现简单，快速上线
- 用户可以自定义封面

**缺点**：
- 列表加载慢（加载大图）
- 流量消耗大

---

### 阶段二：优化版（推荐）

**自动生成缩略图**：

#### 1. 从封面生成缩略图

```java
// 在 uploadCoverImage 后自动生成缩略图
public Result<String> uploadCoverImage(MultipartFile file) {
    // 1. 上传原始封面
    String coverUrl = storageStrategy.storeFile(file, "videos/covers/cover_xxx.jpg");
    
    // 2. 生成缩略图（压缩版本）
    String thumbnailUrl = videoProcessingService.generateThumbnailFromCover(file);
    
    return Result.success(coverUrl);
}
```

#### 2. 从视频截取缩略图（如果用户未上传封面）

```java
// 使用 FFmpeg
public String generateThumbnail(File videoFile, int timestamp) {
    String outputPath = "videos/thumbnails/thumb_" + UUID.randomUUID() + ".jpg";
    
    ProcessBuilder pb = new ProcessBuilder(
        "ffmpeg",
        "-i", videoFile.getAbsolutePath(),
        "-ss", String.valueOf(timestamp),
        "-vframes", "1",
        "-vf", "scale=320:180",
        outputPath
    );
    
    pb.start().waitFor();
    return storageStrategy.getFileUrl(outputPath);
}
```

---

### 阶段三：完整版（长期）

**添加预览片段**：

```java
public String generatePreview(File videoFile, int duration) {
    String outputPath = "videos/previews/preview_" + UUID.randomUUID() + ".mp4";
    
    ProcessBuilder pb = new ProcessBuilder(
        "ffmpeg",
        "-i", videoFile.getAbsolutePath(),
        "-t", String.valueOf(duration),
        "-vf", "scale=480:270",
        "-b:v", "500k",
        "-an", // 移除音频
        outputPath
    );
    
    pb.start().waitFor();
    return storageStrategy.getFileUrl(outputPath);
}
```

**前端使用**：
```vue
<div class="video-card" 
     @mouseenter="playPreview" 
     @mouseleave="stopPreview">
  <img v-if="!isHovering" :src="video.thumbnailUrl" />
  <video v-else :src="video.previewUrl" autoplay loop muted />
</div>
```

---

## 技术选型

### 方案A：FFmpeg（推荐）

**优点**：
- 功能强大，支持所有视频格式
- 性能优秀
- 开源免费

**缺点**：
- 需要安装 FFmpeg
- 需要服务器资源

**安装**：
```bash
# Mac
brew install ffmpeg

# Ubuntu
sudo apt install ffmpeg

# Docker
FROM openjdk:17
RUN apt-get update && apt-get install -y ffmpeg
```

---

### 方案B：云服务（适合大规模）

使用阿里云OSS、腾讯云COS的视频处理服务：
- 自动生成缩略图
- 自动转码
- CDN加速

**优点**：
- 无需自己处理
- 自动优化

**缺点**：
- 需要付费
- 依赖第三方

---

## 推荐实施路线

### 第一步：使用封面作为缩略图（已完成）✅
- `cover_url` = 用户上传的封面
- `thumbnail_url` = `cover_url`（前端自动缩放）

### 第二步：添加缩略图压缩（下一步）
- 从 `cover_url` 生成 320x180 的缩略图
- 存储到 `thumbnail_url`

### 第三步：添加预览片段（可选）
- 异步任务生成预览视频
- 存储到 `preview_url`

---

## 前端优化建议

### 使用响应式图片

```vue
<img 
  :src="video.thumbnailUrl" 
  :srcset="`
    ${video.thumbnailUrl} 320w,
    ${video.coverUrl} 1280w
  `"
  sizes="(max-width: 768px) 320px, 640px"
/>
```

### 懒加载

```vue
<img 
  :src="video.thumbnailUrl" 
  loading="lazy"
  @error="handleImageError"
/>
```

---

## 总结

**当前阶段**：
- 只使用 `cover_url`（用户上传）
- `thumbnail_url` 可以等于 `cover_url` 或留空

**下一步优化**：
- 添加缩略图自动生成（从封面压缩）
- 提升列表加载速度

**长期规划**：
- 添加预览片段（悬停播放）
- 使用云服务处理（如果流量大）

需要我帮您实现缩略图自动生成功能吗？
