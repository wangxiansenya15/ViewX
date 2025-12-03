# 异常处理改造指南

## 📋 改造完成内容

### 1. 新增异常类型

- **`BusinessException`**: 业务异常基类，所有业务异常都继承它
- **`DatabaseException`**: 数据库操作异常 (500)
- **`ExternalServiceException`**: 外部服务调用异常 (503)，如 AI API、邮件服务
- **`ConflictException`**: 操作冲突异常 (409)，如重复点赞、重复注册

### 2. 增强的 GlobalExceptionHandler

现在支持：
- 统一的日志记录（warn 用于业务异常，error 用于系统异常）
- 更精确的 HTTP 状态码映射
- 更友好的错误提示

## 🔧 需要重构的 Service 列表

根据代码扫描，以下 Service 存在过度 try-catch 的问题：

### 高优先级（建议立即重构）

1. **`UserService.java`** - 5处
   - `insertUser`: 应抛出 `ConflictException`（用户名已存在）
   - `getUserById`: 应抛出 `ResourceNotFoundException`
   - `updateUserPassword`: 应抛出 `DatabaseException`

2. **`AuthenticationService.java`** - 3处
   - `verifyCode`: 应抛出 `ValidationException`
   - `resetPassword`: 应抛出 `ValidationException` 或 `ResourceNotFoundException`

3. **`AIService.java`** - 2处
   - `analyzeVideoContent`: 应抛出 `ExternalServiceException`
   - `generateEmbedding`: 应抛出 `ExternalServiceException`

### 中优先级

4. **`FavoriteService.java`** - 6处
5. **`EmailService.java`** - 1处
6. **`ProfileService.java`** - 1处

### 低优先级（消费者可以保留 try-catch）

7. **`ActionLogConsumer.java`** - 消费者失败不应影响主流程，可保留
8. **`NotificationConsumer.java`** - 同上

## 📝 重构示例

### 重构前（UserService）
```java
public Result<User> getUserById(Long id) {
    try {
        User user = userMapper.selectUserById(id);
        return user != null ? Result.success(user) : Result.error(404, "用户不存在");
    } catch (Exception e) {
        log.error("获取用户失败", e);
        return Result.ServerError("获取用户失败");
    }
}
```

### 重构后
```java
public User getUserById(Long id) {
    User user = userMapper.selectUserById(id);
    if (user == null) {
        throw new ResourceNotFoundException("用户不存在: " + id);
    }
    return user;
}
```

Controller 层调用：
```java
@GetMapping("/{id}")
public Result<User> getUser(@PathVariable Long id) {
    User user = userService.getUserById(id);  // 异常会被全局处理器捕获
    return Result.success(user);
}
```

## ✅ 重构原则

1. **Service 层不返回 Result**：直接返回业务对象或抛出异常
2. **Controller 层包装 Result**：只在 Controller 中使用 `Result.success()`
3. **让异常自然抛出**：不要捕获后再包装成 Result
4. **精确的异常类型**：用 `ConflictException` 而不是泛泛的 `Exception`

## 🚫 不需要重构的场景

- MQ 消费者（失败不应影响其他消息）
- 定时任务（失败后继续下一次调度）
- 异步任务（需要记录日志但不中断）
