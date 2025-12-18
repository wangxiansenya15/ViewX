# WebSocket 错误消息推送功能说明

## 功能概述
当用户发送聊天消息失败时（例如非互关用户限制、消息被拦截等），系统会通过 WebSocket 将错误信息实时推送给前端，并以 ElMessage 的形式显示给用户。

## 问题背景
之前的实现中，当消息发送失败时（如非互关用户只能发送一条消息的限制），后端虽然记录了错误日志，但没有将错误信息返回给前端，导致用户不知道发送失败的原因。

```
// 之前的日志
2025-12-17T23:13:54.718+08:00  INFO 36200 --- [boundChannel-58] c.f.v.c.ChatWebSocketController          : 消息保存结果: code=403, message=非互关用户只能发送一条消息，请先互相关注
2025-12-17T23:13:54.718+08:00 ERROR 36200 --- [boundChannel-58] c.f.v.c.ChatWebSocketController          : ❌ 消息保存失败: 非互关用户只能发送一条消息，请先互相关注
// 但前端用户看不到任何提示
```

## 解决方案

### 1. 后端修改 (ChatWebSocketController.java)

#### 消息保存失败时的处理
```java
} else {
    // 消息保存失败，将错误信息发送给发送者
    log.error("❌ 消息保存失败: {}", result.getMessage());
    
    // 构造错误响应
    java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
    errorResponse.put("type", "error");
    errorResponse.put("code", result.getCode());
    errorResponse.put("message", result.getMessage());
    errorResponse.put("timestamp", java.time.LocalDateTime.now());
    
    // 序列化错误响应
    String errorJson = objectMapper.writeValueAsString(errorResponse);
    
    // 发送错误消息给发送者
    messagingTemplate.convertAndSendToUser(
            username,
            "/queue/errors",  // 使用专门的错误队列
            errorJson);
    
    log.info("✅ 错误消息已发送给发送者: {}", username);
}
```

#### 异常捕获时的处理
```java
} catch (Exception e) {
    log.error("❌ WebSocket 发送消息失败", e);
    
    // 尝试发送通用错误消息给用户
    try {
        String username = principal.getName();
        java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
        errorResponse.put("type", "error");
        errorResponse.put("code", 500);
        errorResponse.put("message", "消息发送失败: " + e.getMessage());
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        
        String errorJson = objectMapper.writeValueAsString(errorResponse);
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                errorJson);
    } catch (Exception ex) {
        log.error("❌ 发送错误消息失败", ex);
    }
}
```

### 2. 前端修改 (websocket.ts)

#### 添加错误回调数组
```typescript
private errorCallbacks: ((error: any) => void)[] = []
```

#### 订阅错误队列
```typescript
// 订阅错误消息
this.client.subscribe('/user/queue/errors', (message: IMessage) => {
    try {
        const errorData = JSON.parse(message.body)
        console.error('❌ 收到错误消息:', errorData)
        
        // 显示错误提示
        ElMessage.error(errorData.message || '操作失败')
        
        // 调用错误回调
        this.errorCallbacks.forEach(cb => cb(errorData))
    } catch (error) {
        console.error('❌ 解析错误消息失败:', error)
    }
})
console.log('✅ 已订阅: /user/queue/errors')
```

#### 添加注册错误回调的方法
```typescript
/**
 * 注册错误回调
 */
onError(callback: (error: any) => void) {
    this.errorCallbacks.push(callback)
}
```

#### 清理时重置错误回调
```typescript
disconnect() {
    if (this.client) {
        // ... 其他清理
        this.errorCallbacks = []
        console.log('WebSocket 已断开')
    }
}
```

## 错误消息格式

### 后端发送的错误消息格式
```json
{
    "type": "error",
    "code": 403,
    "message": "非互关用户只能发送一条消息，请先互相关注",
    "timestamp": "2025-12-17T23:13:54.718"
}
```

### 前端接收处理
1. 解析 JSON 消息
2. 使用 `ElMessage.error()` 显示错误提示
3. 调用所有注册的错误回调函数

## 使用场景

### 1. 非互关用户限制
```
用户A 向 用户B 发送第二条消息
↓
后端检测到非互关且已发送过消息
↓
返回 403 错误: "非互关用户只能发送一条消息，请先互相关注"
↓
前端显示红色错误提示
```

### 2. 消息内容违规
```
用户发送包含敏感词的消息
↓
后端内容审核失败
↓
返回 400 错误: "消息包含违规内容"
↓
前端显示错误提示
```

### 3. 系统异常
```
消息保存到数据库失败
↓
后端捕获异常
↓
返回 500 错误: "消息发送失败: [具体错误]"
↓
前端显示错误提示
```

## 扩展使用

### 在组件中注册错误回调
```typescript
import { webSocketService } from '@/utils/websocket'

// 在组件中注册自定义错误处理
webSocketService.onError((error) => {
    console.log('收到错误:', error)
    
    // 根据错误类型做不同处理
    if (error.code === 403) {
        // 显示关注提示
        showFollowDialog()
    } else if (error.code === 400) {
        // 显示内容审核提示
        showContentWarning()
    }
})
```

## 日志示例

### 成功发送错误消息
```
2025-12-17 23:13:54 ERROR - ❌ 消息保存失败: 非互关用户只能发送一条消息，请先互相关注
2025-12-17 23:13:54 INFO  - ✅ 错误消息已发送给发送者: user123
```

### 前端接收错误消息
```
❌ 收到错误消息: {
  type: 'error',
  code: 403,
  message: '非互关用户只能发送一条消息，请先互相关注',
  timestamp: '2025-12-17T23:13:54.718'
}
```

## 优势

1. **实时反馈**: 用户立即知道消息发送失败的原因
2. **用户体验**: 清晰的错误提示，不会让用户困惑
3. **可扩展性**: 可以根据不同错误码做不同处理
4. **统一处理**: 所有 WebSocket 错误都通过同一个队列处理
5. **降级保护**: 即使错误消息发送失败，也不会影响主流程

## 注意事项

1. **错误消息不要过于技术化**: 应该是用户友好的提示
2. **避免敏感信息泄露**: 不要在错误消息中包含系统内部信息
3. **错误码规范**: 使用标准的 HTTP 状态码
4. **日志记录**: 后端应该记录详细的错误日志用于调试

## 相关文件

- `ChatWebSocketController.java` - 后端 WebSocket 控制器
- `websocket.ts` - 前端 WebSocket 服务
- `Messages.vue` - 聊天界面组件

## 总结

通过这个改进，用户在发送消息失败时能够立即收到清晰的错误提示，大大提升了用户体验。同时，这个机制也为未来的功能扩展（如消息审核、限流提示等）提供了基础。
