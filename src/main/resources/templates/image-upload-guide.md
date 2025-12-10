# ViewX 图片和图片集功能使用指南

## 功能概述

ViewX 现已支持**图片**和**图片集**的上传和展示,让内容更加丰富多样!

### 支持的内容类型
- ✅ **单张图片** - 分享精美的照片、插画、设计作品
- ✅ **图片集** - 上传 2-9 张图片,讲述完整的故事
- ✅ **视频** - 原有的视频上传功能

## 快速开始

### 1. 数据库初始化

首先需要创建 `vx_contents` 表:

```bash
# 连接到数据库
psql -U viewx_user -d viewx_db

# 执行建表脚本
\i src/main/resources/sql/contents.sql
```

### 2. 上传单张图片

#### 前端使用

```vue
<template>
  <UploadImage @publish-success="handleSuccess" />
</template>

<script setup>
import UploadImage from '@/views/UploadImage.vue'

const handleSuccess = () => {
  console.log('图片发布成功!')
  // 刷新列表或跳转
}
</script>
```

#### API 调用

```typescript
import { contentApi } from '@/api'

// 上传单张图片
const file = document.querySelector('input[type="file"]').files[0]
const result = await contentApi.uploadImage(file, {
  title: '美丽的风景',
  description: '在山顶拍摄的日出',
  category: '摄影',
  tags: ['风景', '日出', '自然'],
  visibility: 'PUBLIC'
})

console.log('内容ID:', result)
```

#### cURL 示例

```bash
curl -X POST http://localhost:8080/api/contents/image \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/image.jpg" \
  -F "title=美丽的风景" \
  -F "description=在山顶拍摄的日出" \
  -F "category=摄影" \
  -F "tags=风景" \
  -F "tags=日出" \
  -F "visibility=PUBLIC"
```

### 3. 上传图片集

#### 前端使用

```vue
<template>
  <UploadImage @publish-success="handleSuccess" />
</template>
```

组件会自动处理单张图片和图片集的切换。

#### API 调用

```typescript
import { contentApi } from '@/api'

// 上传图片集 (2-9张)
const files = Array.from(document.querySelector('input[type="file"]').files)
const result = await contentApi.uploadImageSet(files, {
  title: '旅行日记',
  description: '这次旅行的精彩瞬间',
  category: '旅行',
  tags: ['旅行', '摄影'],
  visibility: 'PUBLIC'
})

console.log('内容ID:', result)
```

#### cURL 示例

```bash
curl -X POST http://localhost:8080/api/contents/image-set \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "files=@/path/to/image1.jpg" \
  -F "files=@/path/to/image2.jpg" \
  -F "files=@/path/to/image3.jpg" \
  -F "title=旅行日记" \
  -F "description=这次旅行的精彩瞬间" \
  -F "visibility=PUBLIC"
```

### 4. 查询内容

#### 获取内容详情

```typescript
// 获取单个内容的详细信息
const content = await contentApi.getContentDetail(contentId)

console.log(content.contentType)  // 'IMAGE' | 'IMAGE_SET' | 'VIDEO'
console.log(content.primaryUrl)   // 主图片URL
console.log(content.mediaUrls)    // 图片集的所有图片URL
```

#### 获取用户内容列表

```typescript
// 获取用户的所有内容
const allContents = await contentApi.getUserContents(userId)

// 只获取图片
const images = await contentApi.getUserContents(userId, 'IMAGE')

// 只获取图片集
const imageSets = await contentApi.getUserContents(userId, 'IMAGE_SET')

// 获取我的内容
const myContents = await contentApi.getMyContents()
const myImages = await contentApi.getMyContents('IMAGE')
```

## API 接口文档

### 上传单张图片
```
POST /api/contents/image
Content-Type: multipart/form-data

参数:
- file: File (必需) - 图片文件
- title: String (必需) - 标题
- description: String (可选) - 描述
- category: String (可选) - 分类
- subcategory: String (可选) - 子分类
- tags: String[] (可选) - 标签数组
- visibility: String (可选) - PUBLIC | PRIVATE | UNLISTED

返回: Long - 内容ID
```

### 上传图片集
```
POST /api/contents/image-set
Content-Type: multipart/form-data

参数:
- files: File[] (必需) - 图片文件数组 (2-9张)
- title: String (必需) - 标题
- description: String (可选) - 描述
- category: String (可选) - 分类
- subcategory: String (可选) - 子分类
- tags: String[] (可选) - 标签数组
- visibility: String (可选) - PUBLIC | PRIVATE | UNLISTED

返回: Long - 内容ID
```

### 获取内容详情
```
GET /api/contents/{id}

返回: ContentDetailVO
{
  id: number
  contentType: 'VIDEO' | 'IMAGE' | 'IMAGE_SET'
  title: string
  description: string
  primaryUrl: string        // 主图片URL
  coverUrl: string          // 封面URL
  thumbnailUrl: string      // 缩略图URL
  mediaUrls: string[]       // 图片集的所有图片URL
  uploaderId: number
  uploaderNickname: string
  uploaderAvatar: string
  viewCount: number
  likeCount: number
  commentCount: number
  isLiked: boolean
  isFavorited: boolean
  createdAt: string
  publishedAt: string
}
```

### 获取用户内容列表
```
GET /api/contents/user/{userId}?type=IMAGE

参数:
- type: String (可选) - VIDEO | IMAGE | IMAGE_SET

返回: ContentVO[]
```

### 获取我的内容列表
```
GET /api/contents/my?type=IMAGE

参数:
- type: String (可选) - VIDEO | IMAGE | IMAGE_SET

返回: ContentVO[]
```

### 删除内容
```
DELETE /api/contents/{id}

返回: String - 成功消息
```

## 文件限制

### 单张图片
- **支持格式**: JPG, JPEG, PNG, GIF, WEBP
- **最大大小**: 10MB
- **自动生成**: 缩略图 (320x180)

### 图片集
- **图片数量**: 2-9 张
- **单张大小**: 最大 10MB
- **支持格式**: JPG, JPEG, PNG, GIF, WEBP
- **自动生成**: 使用第一张图片生成封面和缩略图

## 前端组件

### UploadImage.vue

功能特性:
- ✅ 单张图片/图片集切换
- ✅ 拖拽上传
- ✅ 实时预览
- ✅ 图片数量验证
- ✅ 文件大小验证
- ✅ 表单填写
- ✅ 美观的UI设计

使用示例:
```vue
<template>
  <UploadImage @publish-success="handlePublishSuccess" />
</template>

<script setup>
import UploadImage from '@/views/UploadImage.vue'

const handlePublishSuccess = () => {
  // 处理发布成功
  router.push('/profile')
}
</script>
```

## 展示组件

### ContentCard.vue (待创建)

用于统一展示各种类型的内容:

```vue
<template>
  <div class="content-card">
    <!-- 视频内容 -->
    <video v-if="content.contentType === 'VIDEO'" :src="content.primaryUrl" />
    
    <!-- 单张图片 -->
    <img v-else-if="content.contentType === 'IMAGE'" :src="content.primaryUrl" />
    
    <!-- 图片集 -->
    <div v-else-if="content.contentType === 'IMAGE_SET'" class="image-set">
      <img :src="content.primaryUrl" />
      <span class="image-count">{{ content.mediaUrls.length }} 张图片</span>
    </div>
  </div>
</template>
```

## 数据库表结构

### vx_contents 表

```sql
CREATE TABLE vx_contents (
    id BIGINT PRIMARY KEY,
    content_type VARCHAR(20) NOT NULL,  -- VIDEO, IMAGE, IMAGE_SET
    title VARCHAR(200) NOT NULL,
    description TEXT,
    primary_url VARCHAR(500) NOT NULL,  -- 主要媒体URL
    cover_url VARCHAR(500),             -- 封面图
    thumbnail_url VARCHAR(500),         -- 缩略图
    media_urls VARCHAR(500)[],          -- 图片集的所有图片URL
    duration INTEGER,                   -- 视频专用
    uploader_id BIGINT NOT NULL,
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    comment_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    published_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);
```

## 最佳实践

### 1. 图片优化建议
- 上传前压缩图片,减少文件大小
- 使用合适的图片格式 (照片用JPG,插画用PNG)
- 避免上传过大的原图

### 2. 图片集使用场景
- 📸 旅行相册 - 记录旅行的精彩瞬间
- 🎨 作品集 - 展示设计或艺术作品
- 📖 教程步骤 - 图文并茂的教程
- 🍔 美食日记 - 分享美食制作过程

### 3. 标题和描述
- 使用有吸引力的标题
- 添加详细的描述,提高搜索可见性
- 使用相关的标签和分类

### 4. 隐私设置
- `PUBLIC` - 所有人可见
- `PRIVATE` - 仅自己可见
- `UNLISTED` - 有链接的人可见

## 故障排查

### 上传失败
1. 检查文件格式是否支持
2. 确认文件大小不超过限制
3. 检查网络连接
4. 查看浏览器控制台错误信息

### 图片不显示
1. 确认图片URL是否正确
2. 检查存储服务是否正常
3. 验证权限设置

### 图片集数量限制
- 最少 2 张图片
- 最多 9 张图片
- 超出范围会提示错误

## 后续计划

- [ ] 图片编辑功能 (裁剪、滤镜)
- [ ] 图片水印
- [ ] 批量上传优化
- [ ] 图片压缩优化
- [ ] CDN 加速
- [ ] 图片懒加载
- [ ] 瀑布流展示
- [ ] 图片搜索优化

## 示例代码

完整的使用示例请参考:
- 后端: `ContentController.java`
- 前端: `UploadImage.vue`
- API: `src/api/index.ts` 中的 `contentApi`

## 支持

如有问题,请查看:
- 📖 [API 文档](./api-documentation.md)
- 🐛 [问题反馈](https://github.com/your-repo/issues)
- 💬 [社区讨论](https://github.com/your-repo/discussions)
