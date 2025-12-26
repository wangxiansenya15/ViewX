# FFmpeg执行器重构总结

## ✅ 完成的工作

### 1. **创建策略模式架构**

#### 接口层
- ✅ `FFmpegExecutor.java` - 执行器接口

#### 实现层
- ✅ `DockerFFmpegExecutor.java` - Docker容器方式
- ✅ `NativeFFmpegExecutor.java` - 原生Linux方式

#### 服务层
- ✅ `VideoProcessingServiceImpl.java` - 重构使用策略模式

---

## 📁 文件结构

```
src/main/java/com/flowbrain/viewx/
├── service/
│   ├── VideoProcessingService.java          # 接口（无变化）
│   ├── ffmpeg/                               # 新增包
│   │   ├── FFmpegExecutor.java              # 策略接口
│   │   ├── DockerFFmpegExecutor.java        # Docker实现
│   │   └── NativeFFmpegExecutor.java        # 原生实现
│   └── impl/
│       └── VideoProcessingServiceImpl.java   # 重构（使用策略）

src/main/resources/
└── application.yml                           # 添加FFmpeg配置
```

---

## 🎯 设计模式：策略模式

### 类图

```
┌─────────────────────────┐
│  FFmpegExecutor        │ ◄─────────┐
│  (Interface)           │            │
├─────────────────────────┤            │
│ + execute()            │            │
│ + extractFrame()       │            │
│ + getExecutorType()    │            │
└─────────────────────────┘            │
           △                           │
           │                           │
    ┌──────┴──────┐                   │
    │             │                   │
┌───┴────┐   ┌───┴────┐              │
│ Docker │   │ Native │              │
│ Exec   │   │ Exec   │              │
└────────┘   └────────┘              │
                                      │
                                      │
┌─────────────────────────────────────┴─┐
│  VideoProcessingServiceImpl           │
├───────────────────────────────────────┤
│  - ffmpegExecutor: FFmpegExecutor    │
├───────────────────────────────────────┤
│  + generateThumbnail()                │
│  + generateThumbnailFromCover()       │
└───────────────────────────────────────┘
```

---

## 🔧 配置方式

### Docker方式（默认）

```yaml
ffmpeg:
  executor:
    type: docker
```

### 原生方式

```yaml
ffmpeg:
  executor:
    type: native
  binary:
    path: /usr/bin/ffmpeg  # 可选
```

---

## 💻 代码对比

### 重构前（硬编码Docker）

```java
@Override
public String generateThumbnail(File videoFile, int timestamp) {
    // 硬编码Docker命令
    ProcessBuilder pb = new ProcessBuilder(
        "docker", "exec", "viewx-ffmpeg",
        "ffmpeg",
        "-ss", String.valueOf(timestamp),
        "-i", "/workdir/" + videoFileName,
        "-vframes", "1",
        "-q:v", "2",
        "-y",
        outputPath
    );
    
    Process process = pb.start();
    // ... 处理输出
}
```

**问题**:
- ❌ 硬编码Docker命令
- ❌ 无法切换到原生FFmpeg
- ❌ 违反开闭原则

### 重构后（策略模式）

```java
@Autowired
private FFmpegExecutor ffmpegExecutor;

@Override
public String generateThumbnail(File videoFile, int timestamp) {
    log.info("使用{}方式提取视频关键帧", 
            ffmpegExecutor.getExecutorType());
    
    // 使用策略模式
    String outputPath = videoFile.getParent();
    String thumbnailFileName = ffmpegExecutor.extractFrame(
        videoFile, timestamp, outputPath
    );
    
    return thumbnailFileName;
}
```

**优点**:
- ✅ 业务逻辑与执行方式解耦
- ✅ 通过配置切换执行方式
- ✅ 符合开闭原则
- ✅ 易于扩展（如添加云端FFmpeg）

---

## 🔄 自动切换机制

### Spring条件注解

```java
// Docker执行器
@ConditionalOnProperty(
    name = "ffmpeg.executor.type", 
    havingValue = "docker", 
    matchIfMissing = true  // 默认值
)
public class DockerFFmpegExecutor implements FFmpegExecutor {
    // ...
}

// 原生执行器
@ConditionalOnProperty(
    name = "ffmpeg.executor.type", 
    havingValue = "native"
)
public class NativeFFmpegExecutor implements FFmpegExecutor {
    // ...
}
```

**工作原理**:
1. Spring启动时读取配置 `ffmpeg.executor.type`
2. 根据配置值注入对应的执行器实现
3. 业务代码无感知，自动使用正确的执行器

---

## 📊 执行流程对比

### Docker方式

```
VideoService
    ↓
VideoProcessingService
    ↓
DockerFFmpegExecutor
    ↓
docker exec viewx-ffmpeg ffmpeg ...
    ↓
容器内FFmpeg执行
    ↓
返回结果
```

**路径**: `/workdir/video.mp4` (容器内路径)

### 原生方式

```
VideoService
    ↓
VideoProcessingService
    ↓
NativeFFmpegExecutor
    ↓
/usr/bin/ffmpeg ...
    ↓
系统FFmpeg执行
    ↓
返回结果
```

**路径**: `/var/www/html/viewx/videos/123/456/source.mp4` (绝对路径)

---

## ✨ 优势总结

### 1. **业务解耦**
- 业务代码不关心FFmpeg如何执行
- 只需调用 `videoProcessingService.generateThumbnail()`

### 2. **配置驱动**
- 通过配置文件切换执行方式
- 无需修改代码

### 3. **易于扩展**
- 添加新的执行方式只需实现 `FFmpegExecutor` 接口
- 例如：云端FFmpeg、GPU加速等

### 4. **环境适配**
- 开发环境：Docker方式（环境一致）
- 生产环境：原生方式（性能最佳）

### 5. **符合SOLID原则**
- **单一职责**: 每个执行器只负责一种方式
- **开闭原则**: 对扩展开放，对修改关闭
- **依赖倒置**: 依赖抽象（接口）而非具体实现

---

## 🚀 使用示例

### 业务代码（无需修改）

```java
@Autowired
private VideoProcessingService videoProcessingService;

public void processVideo(File videoFile) {
    // 自动使用配置的执行器
    String thumbnail = videoProcessingService.generateThumbnail(videoFile, 1);
    log.info("生成缩略图: {}", thumbnail);
}
```

### 切换执行方式

**开发环境** (`application-dev.yml`):
```yaml
ffmpeg:
  executor:
    type: docker
```

**生产环境** (`application-prod.yml`):
```yaml
ffmpeg:
  executor:
    type: native
  binary:
    path: /usr/bin/ffmpeg
```

**启动应用**:
```bash
# 开发环境（使用Docker）
java -jar viewx.jar --spring.profiles.active=dev

# 生产环境（使用原生）
java -jar viewx.jar --spring.profiles.active=prod
```

---

## 📝 日志示例

### Docker方式
```
使用docker方式提取视频关键帧: video.mp4, 时间戳: 1秒
执行Docker FFmpeg命令: docker exec viewx-ffmpeg ffmpeg -ss 1 -i /workdir/video.mp4 -vframes 1 -q:v 2 -y /workdir/video_thumb_1.jpg
成功生成视频缩略图: video_thumb_1.jpg
```

### 原生方式
```
使用native方式提取视频关键帧: video.mp4, 时间戳: 1秒
执行原生FFmpeg命令: /usr/bin/ffmpeg -ss 1 -i /var/www/html/viewx/videos/123/456/source.mp4 -vframes 1 -q:v 2 -y /var/www/html/viewx/videos/123/456/source_thumb_1.jpg
成功生成视频缩略图: source_thumb_1.jpg
```

---

## 🎉 总结

### 重构成果

1. ✅ **解耦成功**: 业务逻辑与FFmpeg执行方式完全解耦
2. ✅ **配置驱动**: 通过配置文件灵活切换
3. ✅ **易于扩展**: 符合开闭原则，便于添加新的执行方式
4. ✅ **向后兼容**: 业务代码无需修改
5. ✅ **文档完善**: 提供详细的配置和使用说明

### 下一步建议

- [ ] 添加FFmpeg版本检测
- [ ] 实现GPU加速支持
- [ ] 添加云端FFmpeg支持（如AWS Lambda）
- [ ] 性能监控和统计

---

**完成时间**: 2025-12-24  
**重构文件**: 
- `FFmpegExecutor.java` (新增)
- `DockerFFmpegExecutor.java` (新增)
- `NativeFFmpegExecutor.java` (新增)
- `VideoProcessingServiceImpl.java` (重构)
- `application.yml` (配置)

**文档**:
- `FFMPEG_EXECUTOR_CONFIG.md` (配置说明)
