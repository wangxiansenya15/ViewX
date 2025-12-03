# ViewX 数据层架构优化文档

## 📋 优化概览

本次优化遵循 **三层数据模型** 设计原则，清晰划分了 Entity、DTO、VO 的职责边界。

## 🏗️ 三层架构说明

### 1. Entity (实体层)
**职责**: 与数据库表一一对应，只负责数据持久化。

**优化点**:
- 添加 `@TableName` 注解明确表名映射
- 使用 `@TableId(type = IdType.ASSIGN_ID)` 自动生成雪花ID
- 使用 `@TableField(fill = FieldFill.INSERT)` 自动填充创建时间
- 使用 `@TableField(fill = FieldFill.INSERT_UPDATE)` 自动填充更新时间
- 使用 `@TableField(exist = false)` 标记非数据库字段（如关联查询的冗余字段）
- 使用 `JacksonTypeHandler` 处理 JSONB 类型（tags, aiTags）

**已优化实体**:
- `Video` - 视频主表
- `VideoComment` - 评论表
- `Notification` - 通知表

### 2. DTO (Data Transfer Object)
**职责**: 用于接收前端请求参数，进行参数校验。

**设计原则**:
- 只包含业务操作所需的字段
- 使用 `@NotNull`, `@NotBlank`, `@Size` 等注解进行参数校验
- 不包含数据库自动生成的字段（如 ID、时间戳）

**已创建 DTO**:
- `VideoCreateDTO` - 视频上传
- `VideoUpdateDTO` - 视频编辑
- `CommentCreateDTO` - 评论发布

### 3. VO (View Object)
**职责**: 返回给前端的数据模型，根据不同场景定制化展示。

**设计原则**:
- 根据前端页面需求定制字段
- 可以包含多表关联的冗余字段
- 可以包含计算字段（如 `isLiked`）

**已创建 VO**:
- `VideoListVO` - 视频列表（精简版）
- `VideoDetailVO` - 视频详情（完整版，包含用户交互状态）
- `CommentVO` - 评论（支持嵌套回复）
- `NotificationVO` - 通知
- `UserBriefVO` - 用户简要信息

## 🔄 数据流转示例

### 场景1: 用户上传视频
```
前端 -> VideoCreateDTO (参数校验) -> Service 层转换 -> Video Entity -> 数据库
数据库 -> Video Entity -> Service 层转换 -> VideoDetailVO -> 前端
```

### 场景2: 获取视频列表
```
前端请求 -> Service 查询 -> Video Entity (多条) -> 转换为 VideoListVO (精简) -> 前端
```

### 场景3: 查看视频详情
```
前端请求 -> Service 查询 -> Video Entity + 关联查询 -> VideoDetailVO (包含用户交互状态) -> 前端
```

## 📊 字段映射规范

### 数据库命名 (snake_case)
```sql
view_count, like_count, is_read, created_at
```

### Java 命名 (camelCase)
```java
viewCount, likeCount, isRead, createdAt
```

### MyBatis-Plus 自动映射
```java
@TableField(value = "view_count")  // 显式指定（可选，驼峰转换会自动处理）
private Long viewCount;
```

## 🎯 最佳实践

### 1. Controller 层
```java
@PostMapping("/videos")
public Result<VideoDetailVO> createVideo(@Valid @RequestBody VideoCreateDTO dto) {
    // 使用 DTO 接收参数
    VideoDetailVO vo = videoService.createVideo(dto);
    return Result.success(vo);
}
```

### 2. Service 层
```java
public VideoDetailVO createVideo(VideoCreateDTO dto) {
    // DTO -> Entity
    Video video = new Video();
    BeanUtils.copyProperties(dto, video);
    video.setUploaderId(getCurrentUserId());
    
    // 保存
    videoMapper.insert(video);
    
    // Entity -> VO
    return convertToDetailVO(video);
}
```

### 3. 避免的反模式
❌ **错误**: Controller 直接接收 Entity
```java
@PostMapping("/videos")
public Result<Video> create(@RequestBody Video video) { // 不要这样做！
    ...
}
```

✅ **正确**: 使用 DTO
```java
@PostMapping("/videos")
public Result<VideoDetailVO> create(@Valid @RequestBody VideoCreateDTO dto) {
    ...
}
```

## 🔧 待优化项

1. **User 实体**: 添加 MyBatis-Plus 注解
2. **ActionLog 实体**: 添加 MyBatis-Plus 注解
3. **SocialUser 实体**: 添加 MyBatis-Plus 注解
4. **创建更多 DTO**: 如 `UserRegisterDTO`, `UserUpdateDTO`
5. **创建更多 VO**: 如 `UserProfileVO`, `VideoStatisticsVO`

## 📝 数据库优化建议

### 1. 统一时间字段类型
所有表的时间字段统一使用 `TIMESTAMPTZ`（带时区）

### 2. 统一计数字段默认值
```sql
view_count BIGINT DEFAULT 0,
like_count BIGINT DEFAULT 0
```

### 3. 统一布尔字段默认值
```sql
is_read BOOLEAN DEFAULT FALSE,
is_pinned BOOLEAN DEFAULT FALSE
```

### 4. 添加软删除支持
```sql
deleted_at TIMESTAMPTZ,
is_deleted BOOLEAN DEFAULT FALSE
```

对应 Entity:
```java
@TableLogic
private Boolean isDeleted;
```
