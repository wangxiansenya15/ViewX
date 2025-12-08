# 视频上传功能更新说明

## 更新内容

### 1. 数据库变更

#### 新增字段
- `cover_url` VARCHAR(500) - 封面图片URL

#### 字段说明
- `duration` - 视频时长（秒），**必填字段**，由前端解析视频后传入
- `video_url` - 视频文件URL，存储在 `/videos` 目录
- `cover_url` - 封面图片URL，存储在 `/videos/covers` 目录

### 2. 文件存储结构

```
uploads/
├── avatars/          # 用户头像
├── videos/           # 视频文件
│   └── covers/       # 视频封面图片
```

### 3. API接口

#### 3.1 上传视频封面
```
POST /videos/upload/cover
Content-Type: multipart/form-data

参数:
- file: 封面图片文件 (必需)

返回:
{
  "code": 200,
  "message": "success",
  "data": "http://localhost:8080/uploads/videos/covers/cover_1234567890.jpg"
}
```

#### 3.2 上传视频
```
POST /videos
Content-Type: multipart/form-data

参数:
- file: 视频文件 (必需)
- title: 视频标题 (必需)
- duration: 视频时长，单位秒 (必需)
- description: 视频描述 (可选)
- category: 分类 (可选)
- subcategory: 子分类 (可选)
- coverUrl: 封面图片URL (可选，先调用上传封面接口获取)
- thumbnailUrl: 缩略图URL (可选)
- tags: 标签数组 (可选)
- visibility: 可见性 PUBLIC/PRIVATE/UNLISTED (可选，默认PUBLIC)

返回:
{
  "code": 200,
  "message": "success",
  "data": 123456789  // 视频ID
}
```

### 4. 前端上传流程

```javascript
// 步骤1: 用户选择视频文件
const videoFile = event.target.files[0];

// 步骤2: 前端解析视频时长
const duration = await getVideoDuration(videoFile);

// 步骤3: 用户选择或生成封面图片
const coverFile = await generateCoverFromVideo(videoFile);

// 步骤4: 上传封面图片
const coverFormData = new FormData();
coverFormData.append('file', coverFile);

const coverResponse = await fetch('/videos/upload/cover', {
  method: 'POST',
  body: coverFormData,
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const coverResult = await coverResponse.json();
const coverUrl = coverResult.data;

// 步骤5: 上传视频及元数据
const videoFormData = new FormData();
videoFormData.append('file', videoFile);
videoFormData.append('title', '我的视频 #测试');
videoFormData.append('duration', duration);
videoFormData.append('description', '视频描述 #编程');
videoFormData.append('coverUrl', coverUrl);
videoFormData.append('category', 'Education');
videoFormData.append('visibility', 'PUBLIC');

const videoResponse = await fetch('/videos', {
  method: 'POST',
  body: videoFormData,
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const videoResult = await videoResponse.json();
console.log('视频ID:', videoResult.data);
```

### 5. 前端工具函数示例

#### 5.1 获取视频时长
```javascript
function getVideoDuration(file) {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    video.preload = 'metadata';
    
    video.onloadedmetadata = function() {
      window.URL.revokeObjectURL(video.src);
      const duration = Math.floor(video.duration);
      resolve(duration);
    };
    
    video.onerror = function() {
      reject(new Error('无法读取视频信息'));
    };
    
    video.src = URL.createObjectURL(file);
  });
}
```

#### 5.2 从视频生成封面
```javascript
function generateCoverFromVideo(file, timeInSeconds = 1) {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');
    
    video.onloadedmetadata = function() {
      video.currentTime = timeInSeconds;
    };
    
    video.onseeked = function() {
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      context.drawImage(video, 0, 0, canvas.width, canvas.height);
      
      canvas.toBlob(function(blob) {
        window.URL.revokeObjectURL(video.src);
        const coverFile = new File([blob], 'cover.jpg', { type: 'image/jpeg' });
        resolve(coverFile);
      }, 'image/jpeg', 0.9);
    };
    
    video.onerror = function() {
      reject(new Error('无法生成封面'));
    };
    
    video.src = URL.createObjectURL(file);
  });
}
```

### 6. DTO字段说明

#### VideoUploadDTO
```java
public class VideoUploadDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;                    // 视频标题（必填）

    @NotNull(message = "视频时长不能为空")
    @Min(value = 1, message = "视频时长必须大于0")
    private Integer duration;                // 视频时长，单位秒（必填）

    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;              // 视频描述（可选）

    private String coverUrl;                 // 封面图片URL（可选）
    private String thumbnailUrl;             // 缩略图URL（可选）
    private String category;                 // 分类（可选）
    private String subcategory;              // 子分类（可选）
    private String[] tags;                   // 标签数组（可选）
    
    @Pattern(regexp = "PUBLIC|PRIVATE|UNLISTED")
    private String visibility = "PUBLIC";    // 可见性（可选，默认PUBLIC）
}
```

### 7. 文件命名规则

#### 视频文件
- 格式: `video_{userId}_{timestamp}.{extension}`
- 示例: `video_123456_1701936000000.mp4`
- 存储路径: `uploads/videos/video_123456_1701936000000.mp4`

#### 封面图片
- 格式: `cover_{timestamp}.{extension}`
- 示例: `cover_1701936000000.jpg`
- 存储路径: `uploads/videos/covers/cover_1701936000000.jpg`

### 8. 验证规则

#### 视频文件
- 文件类型: 视频格式（mp4, avi, mov等）
- 文件大小: 建议限制（如500MB）

#### 封面图片
- 文件类型: 图片格式（jpg, jpeg, png, webp）
- 文件大小: 建议限制（如5MB）
- 推荐尺寸: 16:9比例（如1920x1080, 1280x720）

### 9. 注意事项

1. **视频时长解析**: 必须由前端在上传前解析视频时长并传入
2. **封面上传**: 建议先上传封面，获取URL后再上传视频
3. **话题提取**: 系统会自动从标题和描述中提取以#开头的话题
4. **文件存储**: 视频和封面分别存储在不同目录，便于管理
5. **事务处理**: 视频上传、记录创建和话题关联在同一事务中

### 10. 错误处理

常见错误码:
- 400: 参数验证失败（如缺少必填字段）
- 401: 未登录
- 500: 服务器错误（如文件存储失败）

示例错误响应:
```json
{
  "code": 400,
  "message": "视频时长不能为空",
  "data": null
}
```
