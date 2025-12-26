# FFmpeg执行器配置说明

## 📋 配置方式

在 `application.yml` 或 `application-{profile}.yml` 中配置：

### 1. Docker容器方式（默认）

```yaml
ffmpeg:
  executor:
    type: docker  # 使用Docker容器中的FFmpeg
```

**要求**:
- Docker已安装并运行
- `viewx-ffmpeg` 容器已启动
- 视频文件挂载到容器的 `/workdir` 目录

**优点**:
- ✅ 环境隔离，不污染宿主机
- ✅ 版本统一，便于管理
- ✅ 资源限制，防止占用过多内存

**缺点**:
- ⚠️ 需要Docker环境
- ⚠️ 文件需要挂载到容器

---

### 2. 原生Linux方式

```yaml
ffmpeg:
  executor:
    type: native  # 使用系统级FFmpeg
  binary:
    path: /usr/bin/ffmpeg  # FFmpeg二进制文件路径（可选，默认为ffmpeg）
```

**要求**:
- 系统已安装FFmpeg

**安装命令**:
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install ffmpeg

# CentOS/RHEL
sudo yum install epel-release
sudo yum install ffmpeg

# 验证安装
ffmpeg -version
```

**优点**:
- ✅ 无需Docker环境
- ✅ 性能更好（无容器开销）
- ✅ 文件访问更直接

**缺点**:
- ⚠️ 需要手动安装FFmpeg
- ⚠️ 版本可能不一致

---

## 🔧 完整配置示例

### application-dev.yml (开发环境 - Docker)

```yaml
ffmpeg:
  executor:
    type: docker
```

### application-prod.yml (生产环境 - 原生)

```yaml
ffmpeg:
  executor:
    type: native
  binary:
    path: /usr/bin/ffmpeg
```

---

## 📊 对比

| 特性 | Docker方式 | 原生方式 |
|------|-----------|---------|
| **环境要求** | Docker | FFmpeg已安装 |
| **性能** | 中等（有容器开销） | 最佳 |
| **资源隔离** | ✅ 是 | ❌ 否 |
| **版本管理** | ✅ 统一 | ⚠️ 依赖系统 |
| **文件访问** | 需要挂载 | 直接访问 |
| **适用场景** | 开发环境、多租户 | 生产环境、高性能 |

---

## 🎯 推荐配置

### 开发环境
```yaml
ffmpeg:
  executor:
    type: docker
```
**原因**: 环境一致性，便于团队协作

### 生产环境（单机部署）
```yaml
ffmpeg:
  executor:
    type: native
  binary:
    path: /usr/bin/ffmpeg
```
**原因**: 性能最佳，资源利用率高

### 生产环境（容器化部署）
```yaml
ffmpeg:
  executor:
    type: docker
```
**原因**: 与整体架构一致

---

## 🔄 切换步骤

### 从Docker切换到原生

1. **安装FFmpeg**
   ```bash
   sudo apt-get install ffmpeg
   ```

2. **修改配置**
   ```yaml
   ffmpeg:
     executor:
       type: native
   ```

3. **重启应用**
   ```bash
   docker-compose restart viewx-backend
   ```

### 从原生切换到Docker

1. **启动FFmpeg容器**
   ```bash
   docker-compose up -d viewx-ffmpeg
   ```

2. **修改配置**
   ```yaml
   ffmpeg:
     executor:
       type: docker
   ```

3. **重启应用**
   ```bash
   docker-compose restart viewx-backend
   ```

---

## 🐛 故障排查

### Docker方式

**问题**: `Cannot connect to the Docker daemon`
```bash
# 检查Docker服务
sudo systemctl status docker

# 启动Docker
sudo systemctl start docker
```

**问题**: `No such container: viewx-ffmpeg`
```bash
# 检查容器状态
docker ps -a | grep ffmpeg

# 启动容器
docker-compose up -d viewx-ffmpeg
```

### 原生方式

**问题**: `ffmpeg: command not found`
```bash
# 检查FFmpeg是否安装
which ffmpeg

# 安装FFmpeg
sudo apt-get install ffmpeg
```

**问题**: `Permission denied`
```bash
# 检查文件权限
ls -l /path/to/video/file

# 修改权限
chmod 644 /path/to/video/file
```

---

## 📝 代码示例

### 使用方式（业务代码无需修改）

```java
@Autowired
private VideoProcessingService videoProcessingService;

// 提取视频关键帧（自动使用配置的执行器）
String thumbnailFileName = videoProcessingService.generateThumbnail(videoFile, 1);
```

### 日志输出

**Docker方式**:
```
使用docker方式提取视频关键帧: video.mp4, 时间戳: 1秒
执行Docker FFmpeg命令: docker exec viewx-ffmpeg ffmpeg -ss 1 -i /workdir/video.mp4 ...
成功生成视频缩略图: video_thumb_1.jpg
```

**原生方式**:
```
使用native方式提取视频关键帧: video.mp4, 时间戳: 1秒
执行原生FFmpeg命令: /usr/bin/ffmpeg -ss 1 -i /path/to/video.mp4 ...
成功生成视频缩略图: video_thumb_1.jpg
```

---

## ✅ 最佳实践

1. **开发环境使用Docker**: 保证团队环境一致
2. **生产环境根据实际情况选择**: 
   - 容器化部署 → Docker方式
   - 物理机/虚拟机部署 → 原生方式
3. **配置文件分离**: 不同环境使用不同的配置文件
4. **监控日志**: 关注FFmpeg执行日志，及时发现问题

---

**最后更新**: 2025-12-24  
**版本**: v1.0
