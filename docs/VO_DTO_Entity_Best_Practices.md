# 📚 VO、DTO、Entity 最佳实践指南

本文档基于 ViewX 项目的用户资料模块，展示 VO、DTO、Entity 的最佳实践。

## 1. 三层对象定义

### 1.1 Entity（实体类）

**职责**：与数据库表一一对应，用于 ORM 映射。

**特点**：
- 包含所有数据库字段（包括敏感信息）
- 使用 MyBatis-Plus 注解（`@TableName`, `@TableId`, `@TableField`）
- **不应该直接返回给前端**

**示例**：
```java
@Data
@TableName("vx_users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String password; // 敏感字段
    private String email;
    private String phone;
    private Role role;
    private Date createdAt;
    
    @TableField(exist = false)
    private UserDetail details; // 关联对象
}
```

### 1.2 DTO（Data Transfer Object）

**职责**：接收前端请求参数，用于数据传输。

**特点**：
- 只包含允许用户提交的字段
- 包含验证注解（`@NotNull`, `@Email`, `@Size` 等）
- 不包含敏感字段（如 ID、密码、角色等）

**示例**：
```java
@Data
public class UserProfileDTO {
    @Size(min = 2, max = 20, message = "昵称长度必须在2-20个字符之间")
    private String nickname;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 200, message = "个人简介不能超过200字")
    private String description;
    
    // 不包含：id, password, role, createdAt 等
}
```

### 1.3 VO（View Object）

**职责**：返回给前端展示的数据模型。

**特点**：
- 只包含前端需要展示的字段
- 隐藏敏感信息（密码、内部 ID 等）
- 可以包含计算字段（如粉丝数、点赞数）
- 字段命名对前端友好

**示例**：
```java
@Data
public class UserProfileVO {
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String role; // 转换为友好名称
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
    
    // 扩展字段
    private Integer followersCount;
    private Integer videoCount;
    
    // 不包含：password, 内部状态字段等
}
```

## 2. 数据流转流程

### 2.1 查询流程（GET）

```
数据库 -> Entity -> Service 转换 -> VO -> Controller -> 前端
```

**代码示例**：
```java
// Service 层
public Result<UserProfileVO> getUserProfile(Long userId) {
    // 1. 查询 Entity
    User user = profileMapper.selectUserProfileById(userId);
    
    // 2. Entity -> VO 转换
    UserProfileVO vo = convertToVO(user);
    
    // 3. 返回 VO
    return Result.success(vo);
}

// Controller 层
@GetMapping("/{userId}")
public Result<UserProfileVO> getUserProfile(@PathVariable Long userId) {
    return profileService.getUserProfile(userId);
}
```

### 2.2 更新流程（PUT/POST）

```
前端 -> DTO -> Controller 验证 -> Service -> Entity -> 数据库
```

**代码示例**：
```java
// Controller 层
@PutMapping("/me")
public Result<UserProfileVO> updateProfile(
        @Valid @RequestBody UserProfileDTO dto, // 自动验证
        Authentication auth) {
    Long userId = getUserIdFromAuth(auth);
    return profileService.updateUserProfile(userId, dto);
}

// Service 层
public Result<UserProfileVO> updateUserProfile(Long userId, UserProfileDTO dto) {
    // 1. 查询现有 Entity
    User user = profileMapper.selectById(userId);
    
    // 2. DTO -> Entity（只更新非空字段）
    if (dto.getNickname() != null) {
        user.setNickname(dto.getNickname());
    }
    
    // 3. 更新数据库
    profileMapper.updateById(user);
    
    // 4. 返回更新后的 VO
    return getUserProfile(userId);
}
```

## 3. 核心原则

### 3.1 分层隔离

| 层级 | 使用对象 | 说明 |
|------|---------|------|
| **Controller** | DTO（入参）、VO（返回） | 永远不直接使用 Entity |
| **Service** | Entity、DTO、VO | 负责转换逻辑 |
| **Mapper** | Entity | 只操作 Entity |

### 3.2 安全性

✅ **正确做法**：
```java
// 返回 VO，隐藏密码
UserProfileVO vo = new UserProfileVO();
vo.setUsername(user.getUsername());
// 不设置 password
```

❌ **错误做法**：
```java
// 直接返回 Entity，暴露密码
return Result.success(user); // 危险！
```

### 3.3 验证规则

**DTO 验证**（Controller 层）：
```java
@PostMapping
public Result<?> create(@Valid @RequestBody UserProfileDTO dto) {
    // Spring 自动验证 DTO 的注解
}
```

**业务验证**（Service 层）：
```java
public Result<?> updateProfile(Long userId, UserProfileDTO dto) {
    // 验证用户是否存在
    if (user == null) {
        return Result.error("用户不存在");
    }
    
    // 验证邮箱是否已被占用
    if (isEmailTaken(dto.getEmail())) {
        return Result.error("邮箱已被占用");
    }
}
```

## 4. 常见场景

### 4.1 场景一：用户注册

```java
// DTO：接收注册信息
@Data
public class UserRegisterDTO {
    @NotBlank
    private String username;
    
    @Size(min = 6, max = 20)
    private String password;
    
    @Email
    private String email;
}

// Service：DTO -> Entity
User user = new User();
user.setUsername(dto.getUsername());
user.setPassword(passwordEncoder.encode(dto.getPassword()));
user.setEmail(dto.getEmail());
user.setRole(Role.USER); // 默认角色
userMapper.insert(user);

// 返回 VO（不包含密码）
return Result.success(convertToVO(user));
```

### 4.2 场景二：视频列表

```java
// VO：列表展示（精简版）
@Data
public class VideoListVO {
    private Long videoId;
    private String title;
    private String coverUrl;
    private Integer viewCount;
    private String authorName; // 关联用户的昵称
}

// VO：详情展示（完整版）
@Data
public class VideoDetailVO extends VideoListVO {
    private String description;
    private List<String> tags;
    private List<CommentVO> comments;
    private Boolean isLiked; // 当前用户是否点赞
}
```

### 4.3 场景三：分页查询

```java
// Service
public Result<Page<VideoListVO>> getVideoList(int page, int size) {
    Page<Video> entityPage = videoMapper.selectPage(
        new Page<>(page, size), null
    );
    
    // Entity Page -> VO Page
    Page<VideoListVO> voPage = new Page<>();
    voPage.setRecords(
        entityPage.getRecords().stream()
            .map(this::convertToListVO)
            .collect(Collectors.toList())
    );
    voPage.setTotal(entityPage.getTotal());
    
    return Result.success(voPage);
}
```

## 5. 工具推荐

### 5.1 对象转换

**方式一：手动转换**（推荐，清晰可控）
```java
private UserProfileVO convertToVO(User user) {
    UserProfileVO vo = new UserProfileVO();
    vo.setUserId(user.getId());
    vo.setUsername(user.getUsername());
    // ... 逐个设置
    return vo;
}
```

**方式二：BeanUtils**（适合字段名完全一致）
```java
UserProfileVO vo = new UserProfileVO();
BeanUtils.copyProperties(user, vo);
vo.setUserId(user.getId()); // 字段名不同需要手动设置
```

**方式三：MapStruct**（大型项目推荐）
```java
@Mapper(componentModel = "spring")
public interface UserConverter {
    UserProfileVO toVO(User user);
    User toEntity(UserProfileDTO dto);
}
```

## 6. 总结

| 对象 | 用途 | 位置 | 特点 |
|------|------|------|------|
| **Entity** | 数据库映射 | Mapper/Service | 包含所有字段，不对外暴露 |
| **DTO** | 接收请求 | Controller 入参 | 包含验证，只含可修改字段 |
| **VO** | 返回响应 | Controller 返回 | 只含展示字段，隐藏敏感信息 |

**核心思想**：
- **Entity 是内部表示**，永远不直接暴露给前端。
- **DTO 是输入契约**，定义前端可以提交什么。
- **VO 是输出契约**，定义前端可以看到什么。
