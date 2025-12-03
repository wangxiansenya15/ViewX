# 开发踩坑记录

本文档汇总了在 ViewX 项目开发过程中遇到的技术难点、陷阱以及解决方案，旨在为后续开发提供参考。

## 1. MyBatis-Plus 字段映射陷阱详解

### 1.1 问题现象
在 `UserMapper` 中使用手写 SQL (`@Select`) 查询用户信息时，发现实体类中的 `registerTime` 字段始终为 `null`，尽管数据库表中的 `created_at` 字段有值。

### 1.2 深度原因分析

#### A. 映射机制的差异
MyBatis-Plus (MP) 和 原生 MyBatis 在处理字段映射时有本质区别：

1.  **MP 内置方法 (`selectById`, `selectOne` 等)**：
    *   **机制**：MP 会解析实体类上的 `@TableName` 和 `@TableField` 注解。
    *   **结果**：它会自动生成包含 `AS` 别名的 SQL 语句（例如 `SELECT created_at AS registerTime ...`），因此能正确映射。

2.  **手写 SQL (`@Select` 或 XML)**：
    *   **机制**：这是**原生 MyBatis** 的行为。原生 MyBatis **不认识** MP 的 `@TableField` 注解。
    *   **依赖**：它完全依赖于 `resultType` 的自动映射规则，或者显式的 `resultMap` 配置。

#### B. 自动映射规则 (Auto-Mapping)
在 `application.yml` 中通常配置了：
```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true # 开启下划线转驼峰
```
这意味着：
*   数据库字段 `user_name` -> 自动查找 Java 属性 `userName` ✅ (匹配成功)
*   数据库字段 `created_at` -> 自动查找 Java 属性 `createdAt` ❌ (匹配失败)

**本项目中的情况**：
*   数据库字段：`created_at`
*   Java 属性：`private Date registerTime;`
*   **冲突点**：`createdAt` (预期) != `registerTime` (实际)。因为属性名不符合标准的驼峰转换规则，原生 MyBatis 无法自动填充。

### 1.3 解决方案全集

#### 方案一：使用 SQL 别名 (最简单，本项目采用)
在 SQL 语句中强制指定别名，使其与 Java 属性名一致。
```java
// ✅ 推荐用于简单查询
@Select("SELECT *, created_at AS registerTime FROM vx_users WHERE id=#{id}")
User selectUserById(Long id);
```

#### 方案二：使用 @Results 注解 (Java 代码中配置映射)
如果字段较多，写别名太长，可以使用注解定义映射关系。
```java
@Select("SELECT * FROM vx_users WHERE id=#{id}")
@Results({
    @Result(column = "created_at", property = "registerTime"),
    @Result(column = "updated_at", property = "updateTime")
    // 其他字段如果符合驼峰规则，会自动映射
})
User selectUserById(Long id);
```

#### 方案三：使用 XML ResultMap (最规范)
在 `UserMapper.xml` 中定义通用的 `ResultMap`，并在查询中引用。
```xml
<resultMap id="BaseResultMap" type="com.flowbrain.viewx.pojo.entity.User">
    <result column="created_at" property="registerTime" />
    <!-- 其他字段 -->
</resultMap>

<select id="selectUserById" resultMap="BaseResultMap">
    SELECT * FROM vx_users WHERE id = #{id}
</select>
```

#### 方案四：规范命名 (治本之策)
修改实体类属性名，使其符合数据库字段的驼峰命名，利用自动映射。
```java
// 修改 Java 属性名
private Date createdAt; // 对应数据库 created_at
```

### 1.4 最佳实践总结
1.  **优先使用 Mybatis-Plus 内置方法**：能用 `userMapper.selectById(id)` 就不要手写 SQL，既简单又不会出错。
2.  **命名规范**：尽量让数据库字段和 Java 属性保持标准的"下划线转驼峰"关系，减少手动配置成本。
3.  **手写 SQL 必查**：一旦手写 SQL，必须检查 `@TableField` 中自定义了名称的字段，这些字段通常都需要特殊处理（别名或 ResultMap）。

---

## 2. 权限控制与 API 设计

### 问题描述
在实现"修改用户资料"功能时，纠结于是在 `ProfileController` 还是 `UserController` 中实现，以及如何处理权限验证。

### 解决方案
采用了**分层验证**的策略：

1. **ProfileController (`/user/profile/me`)**：
   - **面向用户**：普通用户修改自己的资料。
   - **严格验证**：必须校验当前登录用户 ID 是否与要修改的目标 ID 一致。
   - **Service 层验证**：在 `ProfileService` 中添加了 `currentUserId` 和 `currentUserRole` 参数，确保只有本人或管理员能修改。

2. **UserController (`/admin/users/{id}`)**：
   - **面向管理员**：后台管理系统使用。
   - **Spring Security 保护**：路径 `/admin/**` 已配置为仅管理员可访问。
   - **无需额外验证**：信任管理员身份，直接调用 `UserService` 进行更新。

### 最佳实践
- **API 职责分离**：用户端接口与管理端接口分开。
- **防御性编程**：即使有网关/过滤器层的安全保护，业务层（Service）也应保留核心的权限校验逻辑，作为最后一道防线。

---

## 3. 文件上传与路径处理

### 问题描述
在实现本地文件存储策略 (`LocalStorageStrategy`) 时，`extractFilenameFromUrl` 方法存在潜在的 `IndexOutOfBoundsException` 风险，且未处理不带路径的文件名。

### 解决方案
优化了文件名提取逻辑：

```java
private String extractFilenameFromUrl(String fileUrl) {
    if (fileUrl == null || fileUrl.isEmpty()) {
        throw new IllegalArgumentException("文件URL不能为空");
    }
    
    int lastSlashIndex = fileUrl.lastIndexOf('/');
    if (lastSlashIndex == -1) {
        return fileUrl; // 处理没有斜杠的情况
    }
    
    return fileUrl.substring(lastSlashIndex + 1);
}
```

### 最佳实践
- **健壮性检查**：处理字符串操作时，始终考虑空值、空字符串和边界情况（如找不到分隔符）。
- **单元测试**：为工具类方法编写单元测试，覆盖各种边缘情况。

---

## 4. 前后端数据展示不一致

### 问题描述
前端导航栏显示的用户信息（头像、用户名）是静态的，修改个人资料后不会自动更新。

### 解决方案
1. **前端状态管理**：在 `NavBar.vue` 中引入 `userApi`，并在组件挂载 (`onMounted`) 和登录状态变化 (`watch isLoggedIn`) 时主动拉取最新用户信息。
2. **动态渲染**：
   - 头像：`:src="userProfile?.avatarUrl || defaultAvatar"`
   - 用户名：优先显示昵称，没有则显示用户名。

### 最佳实践
- **单一数据源**：尽量从后端获取最新数据，而不是依赖前端缓存的旧数据。
- **响应式更新**：利用 Vue 的 `watch` 和生命周期钩子，确保数据变化时 UI 能够及时响应。

---

## 5. JWT Token 黑名单机制

### 问题描述
JWT 是无状态的，用户登出后 Token 依然有效，存在安全隐患。

### 解决方案
引入 **Redis 黑名单**机制：
1. **登出时**：将 Token 加入 Redis，设置过期时间为 Token 的剩余有效期。
2. **请求拦截时**：`JwtAuthenticationFilter` 先检查 Token 是否在 Redis 黑名单中，如果在则直接拒绝请求。

### 最佳实践
- **安全性与性能平衡**：虽然 JWT 旨在无状态，但在需要高安全性的场景（如注销、修改密码）下，引入轻量级的状态检查（Redis）是必要的权衡。
