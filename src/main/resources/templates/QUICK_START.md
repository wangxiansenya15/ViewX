# 视频上传功能 - 快速开始

## 🚀 快速开始

### 1. 数据库迁移

执行以下SQL脚本：

```bash
# 创建话题表
psql -U your_user -d your_database -f src/main/resources/sql/migrations/002_create_topics_tables.sql

# 添加封面字段
psql -U your_user -d your_database -f src/main/resources/sql/migrations/003_add_cover_url_to_videos.sql
```

或者在PostgreSQL中直接执行：

```sql
-- 创建话题表
CREATE TABLE IF NOT EXISTS vx_topics (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    video_count BIGINT DEFAULT 0,
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vx_video_topics (
    id BIGINT PRIMARY KEY,
    video_id BIGINT NOT NULL REFERENCES vx_videos(id) ON DELETE CASCADE,
    topic_id BIGINT NOT NULL REFERENCES vx_topics(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(video_id, topic_id)
);

-- 添加封面字段
ALTER TABLE vx_videos ADD COLUMN IF NOT EXISTS cover_url VARCHAR(500);
```

### 2. 后端测试

使用Postman或cURL测试：

#### 测试1: 上传封面
```bash
curl -X POST http://localhost:8080/videos/upload/cover \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/cover.jpg"
```

预期响应：
```json
{
  "code": 200,
  "message": "success",
  "data": "http://localhost:8080/uploads/videos/covers/cover_1701936000000.jpg"
}
```

#### 测试2: 上传视频
```bash
curl -X POST http://localhost:8080/videos \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/video.mp4" \
  -F "title=测试视频 #测试" \
  -F "duration=120" \
  -F "description=这是一个测试视频 #编程" \
  -F "coverUrl=http://localhost:8080/uploads/videos/covers/cover_xxx.jpg" \
  -F "category=Education" \
  -F "visibility=PUBLIC"
```

预期响应：
```json
{
  "code": 200,
  "message": "success",
  "data": 123456789
}
```

### 3. 前端集成

#### 安装（如果需要）
```bash
cd ViewX-frontend
npm install
```

#### 使用示例代码
```typescript
import { uploadVideoComplete } from '@/examples/videoUploadExample';

// 在你的组件中
async function handleUpload(videoFile: File) {
  try {
    const token = localStorage.getItem('token');
    
    const videoId = await uploadVideoComplete(
      videoFile,
      {
        title: '我的视频 #测试',
        description: '视频描述 #编程',
        category: 'Education',
        visibility: 'PUBLIC'
      },
      token,
      (step, progress) => {
        console.log(`${step}: ${progress}%`);
      }
    );
    
    console.log('上传成功，视频ID:', videoId);
  } catch (error) {
    console.error('上传失败:', error);
  }
}
```

## 📋 检查清单

### 后端
- [ ] 数据库迁移脚本已执行
- [ ] `vx_topics` 表已创建
- [ ] `vx_video_topics` 表已创建
- [ ] `vx_videos.cover_url` 字段已添加
- [ ] 后端服务已重启
- [ ] `/uploads/videos` 目录存在
- [ ] `/uploads/videos/covers` 目录存在

### 前端
- [ ] 前端代码已更新
- [ ] `videoUploadExample.ts` 文件已添加
- [ ] API基础URL已配置正确
- [ ] 认证token获取正常

## 🧪 测试步骤

### 1. 测试封面上传
1. 准备一张图片（jpg/png）
2. 调用封面上传API
3. 验证返回的URL可以访问
4. 检查文件是否存储在 `/uploads/videos/covers/` 目录

### 2. 测试视频上传
1. 准备一个视频文件（mp4）
2. 使用前端工具函数解析视频时长
3. 上传封面获取URL
4. 上传视频及元数据
5. 验证视频记录已创建
6. 检查话题是否正确提取和关联

### 3. 测试话题提取
1. 上传标题包含 `#测试 #编程` 的视频
2. 查询数据库 `vx_topics` 表
3. 验证话题已创建
4. 查询 `vx_video_topics` 表
5. 验证关联已建立

## 🔍 故障排查

### 问题1: 封面上传失败
**可能原因**:
- 目录不存在
- 权限不足
- 文件类型不支持

**解决方案**:
```bash
# 创建目录
mkdir -p uploads/videos/covers

# 设置权限
chmod 755 uploads/videos/covers
```

### 问题2: 视频上传失败
**可能原因**:
- duration参数缺失
- 文件太大
- 认证失败

**解决方案**:
- 确保前端传入duration参数
- 检查文件大小限制配置
- 验证token是否有效

### 问题3: 话题未提取
**可能原因**:
- 标题/描述中没有#开头的词
- TopicService未正确注入
- 数据库表未创建

**解决方案**:
- 检查标题格式：`我的视频 #话题`
- 验证TopicService bean
- 执行数据库迁移脚本

## 📊 验证数据

### 查询视频记录
```sql
SELECT id, title, duration, video_url, cover_url, created_at 
FROM vx_videos 
ORDER BY created_at DESC 
LIMIT 10;
```

### 查询话题
```sql
SELECT * FROM vx_topics ORDER BY created_at DESC;
```

### 查询视频-话题关联
```sql
SELECT 
    v.id as video_id,
    v.title,
    t.name as topic_name
FROM vx_videos v
JOIN vx_video_topics vt ON v.id = vt.video_id
JOIN vx_topics t ON vt.topic_id = t.id
ORDER BY v.created_at DESC;
```

## 🎯 下一步

1. **前端UI优化**
   - 添加上传进度条
   - 视频预览功能
   - 封面编辑功能

2. **功能增强**
   - 视频转码
   - 多清晰度支持
   - 视频截图生成

3. **性能优化**
   - 异步处理话题提取
   - 添加缓存
   - CDN集成

## 📚 相关文档

- [完整API文档](./VIDEO_UPLOAD_REFACTOR.md)
- [功能总结](./REFACTOR_SUMMARY.md)
- [前端示例代码](./ViewX-frontend/src/examples/videoUploadExample.ts)

## 💡 提示

- 建议先在开发环境测试
- 生产环境需要配置文件大小限制
- 建议使用对象存储（如OSS）替代本地存储
- 定期备份上传的文件

## ✅ 完成

如果以上步骤都成功，恭喜你！视频上传功能已经可以正常使用了。

如有问题，请查看详细文档或联系开发团队。
