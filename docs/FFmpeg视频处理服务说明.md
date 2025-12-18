# FFmpeg 视频处理服务部署说明

## 概述
本项目使用 Docker Compose 部署 FFmpeg 服务，用于视频处理任务（缩略图生成、预览片段生成、视频时长获取）。

## 架构说明

### Docker 服务配置
在 `docker-compose.yml` 中添加了 `ffmpeg` 服务：

```yaml
ffmpeg:
  image: jrottenberg/ffmpeg:4.4-alpine
  container_name: viewx-ffmpeg
  restart: unless-stopped
  environment:
    TZ: Asia/Shanghai
  volumes:
    - ./uploads/videos:/workdir
  networks:
    - viewx-network
  entrypoint: ["/bin/sh", "-c", "while true; do sleep 3600; done"]
  healthcheck:
    test: ["CMD", "ffmpeg", "-version"]
    interval: 30s
    timeout: 10s
    retries: 3
```

### 工作目录映射
- **宿主机路径**: `./uploads/videos`
- **容器内路径**: `/workdir`
- 所有视频文件都存储在 `uploads/videos` 目录下
- Java 应用通过 Docker exec 调用容器内的 FFmpeg 处理这些文件

## 部署步骤

### 1. 启动 FFmpeg 服务

```bash
# 在项目根目录执行
docker-compose up -d ffmpeg
```

### 2. 验证服务状态

```bash
# 检查容器是否运行
docker ps | grep viewx-ffmpeg

# 检查健康状态
docker inspect viewx-ffmpeg | grep -A 5 Health

# 测试 FFmpeg 命令
docker exec viewx-ffmpeg ffmpeg -version
```

### 3. 测试视频处理

```bash
# 假设 uploads/videos 目录下有 test.mp4 文件

# 测试获取视频时长
docker exec viewx-ffmpeg ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 /workdir/test.mp4

# 测试生成缩略图（在第5秒截图）
docker exec viewx-ffmpeg ffmpeg -ss 5 -i /workdir/test.mp4 -vframes 1 -q:v 2 -y /workdir/test_thumb.jpg

# 测试生成预览片段（前10秒）
docker exec viewx-ffmpeg ffmpeg -i /workdir/test.mp4 -t 10 -c:v libx264 -c:a aac -b:v 500k -b:a 128k -y /workdir/test_preview.mp4
```

## Java 服务实现

### VideoProcessingServiceImpl 方法说明

#### 1. generateThumbnail(File videoFile, int timestamp)
从视频的指定时间点生成缩略图。

**参数**:
- `videoFile`: 视频文件对象
- `timestamp`: 截图时间点（秒）

**返回**: 生成的缩略图文件名

**示例**:
```java
File video = new File("uploads/videos/sample.mp4");
String thumbnail = videoProcessingService.generateThumbnail(video, 5);
// 返回: "sample_thumb_5.jpg"
```

#### 2. generatePreview(File videoFile, int duration)
生成视频预览片段（从开头截取指定时长）。

**参数**:
- `videoFile`: 视频文件对象
- `duration`: 预览时长（秒）

**返回**: 生成的预览文件名

**示例**:
```java
File video = new File("uploads/videos/sample.mp4");
String preview = videoProcessingService.generatePreview(video, 10);
// 返回: "sample_preview.mp4"
```

#### 3. getVideoDuration(File videoFile)
获取视频时长。

**参数**:
- `videoFile`: 视频文件对象

**返回**: 视频时长（秒，向上取整）

**示例**:
```java
File video = new File("uploads/videos/sample.mp4");
int duration = videoProcessingService.getVideoDuration(video);
// 返回: 125 (表示 125 秒)
```

## 注意事项

### 1. 文件路径
- Java 代码中传入的 `File` 对象应该指向 `uploads/videos` 目录下的文件
- 容器内的路径会自动映射为 `/workdir/文件名`

### 2. 性能考虑
- FFmpeg 处理是 CPU 密集型操作，建议：
  - 异步处理视频任务
  - 使用消息队列（RabbitMQ）管理处理任务
  - 限制并发处理数量

### 3. 错误处理
- 所有方法都会在失败时抛出 `RuntimeException`
- 建议在调用层添加适当的异常处理和重试机制

### 4. 日志监控
```bash
# 查看 FFmpeg 容器日志
docker logs viewx-ffmpeg

# 实时查看日志
docker logs -f viewx-ffmpeg
```

## 故障排查

### 问题 1: 容器无法启动
```bash
# 检查容器状态
docker ps -a | grep viewx-ffmpeg

# 查看详细日志
docker logs viewx-ffmpeg

# 重新创建容器
docker-compose up -d --force-recreate ffmpeg
```

### 问题 2: 文件找不到
```bash
# 确认文件存在
ls -la uploads/videos/

# 在容器内检查
docker exec viewx-ffmpeg ls -la /workdir/
```

### 问题 3: 权限问题
```bash
# 修改 uploads/videos 目录权限
chmod -R 755 uploads/videos/

# 如果是 Linux，可能需要调整所有者
chown -R 1000:1000 uploads/videos/
```

## 扩展功能

### 添加更多 FFmpeg 功能
可以在 `VideoProcessingServiceImpl` 中添加更多方法，例如：

```java
// 视频转码
public String transcodeVideo(File videoFile, String format) {
    // docker exec viewx-ffmpeg ffmpeg -i input.mp4 -c:v libx264 output.mp4
}

// 添加水印
public String addWatermark(File videoFile, File watermarkImage) {
    // docker exec viewx-ffmpeg ffmpeg -i video.mp4 -i watermark.png -filter_complex overlay output.mp4
}

// 视频压缩
public String compressVideo(File videoFile, int targetBitrate) {
    // docker exec viewx-ffmpeg ffmpeg -i input.mp4 -b:v 1M output.mp4
}
```

## 生产环境建议

1. **使用专用的视频处理服务器**
   - 将 FFmpeg 服务部署在独立的服务器上
   - 通过 API 或消息队列与主应用通信

2. **添加任务队列**
   - 使用 RabbitMQ 管理视频处理任务
   - 实现任务优先级和重试机制

3. **监控和告警**
   - 监控 FFmpeg 容器的 CPU 和内存使用
   - 设置处理超时告警

4. **存储优化**
   - 使用对象存储（如 MinIO、S3）存储视频文件
   - 定期清理临时生成的文件

## 参考资料

- [FFmpeg 官方文档](https://ffmpeg.org/documentation.html)
- [jrottenberg/ffmpeg Docker 镜像](https://hub.docker.com/r/jrottenberg/ffmpeg)
- [FFmpeg 常用命令](https://www.ffmpeg.org/ffmpeg.html)
