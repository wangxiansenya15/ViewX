# ViewX 内容系统升级指南

## 概述

本次升级解决了两个核心问题:
1. **封面上传持久化问题** - 封面URL现在会正确保存到数据库
2. **支持多种内容类型** - 不仅支持视频,还支持图片和图片集

## 第一部分: 封面上传问题修复

### 问题描述
之前的实现中,前端需要调用两个接口:
1. `POST /videos/upload/cover` - 上传封面图片
2. `POST /videos` - 上传视频和元数据

这导致封面URL虽然生成了,但没有保存到数据库的 `vx_videos` 表中。

### 解决方案

#### 后端改动

1. **修改 VideoService 接口**
   - `uploadVideo` 方法新增 `coverFile` 参数,支持同时上传封面
   - `uploadCoverImage` 返回类型改为 `CoverUploadVO`,包含封面URL和缩略图URL

2. **VideoServiceImpl 实现**
   - 在 `uploadVideo` 方法中处理封面上传和缩略图生成
   - 在同一个事务中保存视频URL、封面URL和缩略图URL到数据库
   - 保留 `uploadCoverImage` 作为独立接口供其他场景使用

3. **VideoController 更新**
   - `POST /videos` 接口新增可选参数 `coverFile`
   - `POST /videos/upload/cover` 返回 `CoverUploadVO`

#### 前端改动

1. **API 定义更新** (`src/api/index.ts`)
   ```typescript
   uploadVideo(file: File, coverFile: File | null, data: VideoCreateDTO)
   uploadCoverImage(file: File): Promise<{ coverUrl: string; thumbnailUrl: string }>
   ```

2. **上传流程简化** (`UploadVideo.vue`)
   - 移除单独上传封面的步骤
   - 一次性调用 `uploadVideo`,同时传递视频文件和封面文件
   - 后端统一处理,确保数据一致性

### 使用示例

#### 方式一: 一次性上传(推荐)
```typescript
// 前端代码
await videoApi.uploadVideo(videoFile, coverFile, {
  title: '我的视频',
  duration: 120,
  description: '视频描述'
})
```

#### 方式二: 单独上传封面(兼容旧逻辑)
```typescript
// 先上传封面获取URL
const { coverUrl, thumbnailUrl } = await videoApi.uploadCoverImage(coverFile)

// 再上传视频,传入封面URL
await videoApi.uploadVideo(videoFile, null, {
  title: '我的视频',
  duration: 120,
  coverUrl: coverUrl,
  thumbnailUrl: thumbnailUrl
})
```

## 第二部分: 支持多种内容类型

### 新增 Content 实体

创建了 `Content` 实体类,支持以下内容类型:
- **VIDEO** - 视频内容(原有功能)
- **IMAGE** - 单张图片
- **IMAGE_SET** - 图片集/相册
- **ARTICLE** - 文章(预留)

### 数据库表结构

新表 `vx_contents` 包含:
- `content_type` - 内容类型标识
- `primary_url` - 主要媒体URL(视频URL或主图片URL)
- `media_urls` - 多媒体URL数组(用于图片集)
- `duration` - 视频时长(仅视频类型使用)
- 其他字段与 `vx_videos` 保持一致

### 实施步骤

#### 阶段一: 数据库迁移
```sql
-- 执行 contents.sql 创建新表
psql -U viewx_user -d viewx_db -f src/main/resources/sql/contents.sql
```

#### 阶段二: 创建 ContentService
需要实现以下接口:
```java
public interface ContentService {
    // 上传图片
    Result<Long> uploadImage(Long userId, MultipartFile imageFile, ContentUploadDTO dto);
    
    // 上传图片集
    Result<Long> uploadImageSet(Long userId, List<MultipartFile> imageFiles, ContentUploadDTO dto);
    
    // 获取内容详情(统一接口,支持所有类型)
    Result<ContentDetailVO> getContentDetail(Long contentId, Long userId);
    
    // 获取用户内容列表(可按类型筛选)
    Result<List<ContentVO>> getUserContents(Long userId, String contentType);
}
```

#### 阶段三: 前端支持

1. **新增上传图片组件**
   ```vue
   <template>
     <UploadImage @success="handleImageUploaded" />
     <UploadImageSet @success="handleImageSetUploaded" />
   </template>
   ```

2. **API 定义**
   ```typescript
   export const contentApi = {
     uploadImage(file: File, data: ContentCreateDTO) { ... },
     uploadImageSet(files: File[], data: ContentCreateDTO) { ... },
     getContent(id: number) { ... }
   }
   ```

3. **内容展示组件**
   - 创建 `ContentCard.vue` 统一展示各类内容
   - 根据 `contentType` 渲染不同的UI

### 迁移策略

#### 选项 A: 渐进式迁移(推荐)
1. 保留现有 `vx_videos` 表和 `VideoService`
2. 新内容使用 `vx_contents` 表和 `ContentService`
3. 逐步将视频相关功能迁移到新系统
4. 最终废弃 `vx_videos` 表

#### 选项 B: 数据迁移
```sql
-- 将现有视频数据迁移到 contents 表
INSERT INTO vx_contents (
    id, content_type, title, description, primary_url, cover_url, 
    thumbnail_url, duration, uploader_id, created_at, ...
)
SELECT 
    id, 'VIDEO', title, description, video_url, cover_url,
    thumbnail_url, duration, uploader_id, created_at, ...
FROM vx_videos
WHERE is_deleted = FALSE;
```

## 优势总结

### 修复后的优势
1. ✅ **数据一致性** - 封面URL正确保存到数据库
2. ✅ **简化流程** - 前端只需一次API调用
3. ✅ **事务保证** - 视频和封面在同一事务中处理
4. ✅ **向后兼容** - 保留单独上传封面的接口

### 内容系统优势
1. ✅ **功能丰富** - 支持视频、图片、图片集等多种内容
2. ✅ **统一管理** - 所有内容类型使用统一的数据模型
3. ✅ **易于扩展** - 可轻松添加新的内容类型
4. ✅ **用户体验** - 提供更多样化的内容创作方式

## 测试建议

### 封面上传测试
1. 测试同时上传视频和封面
2. 测试只上传视频(无封面)
3. 测试使用单独上传封面接口
4. 验证数据库中 `cover_url` 和 `thumbnail_url` 字段

### 图片上传测试(待实现)
1. 测试单张图片上传
2. 测试图片集上传(多张图片)
3. 测试图片压缩和缩略图生成
4. 验证不同内容类型的展示

## 后续工作

1. **实现 ContentService** - 完整的图片和图片集上传逻辑
2. **前端组件开发** - 图片上传和展示组件
3. **推荐算法适配** - 支持多种内容类型的推荐
4. **搜索功能增强** - 支持按内容类型搜索
5. **数据迁移工具** - 将现有视频迁移到新系统

## 注意事项

1. **数据库索引** - 确保 `content_type` 字段有索引
2. **存储空间** - 图片集可能占用更多存储空间
3. **性能优化** - 考虑图片压缩和CDN加速
4. **权限控制** - 不同内容类型可能需要不同的权限策略
