---
description: 实现实时私信聊天功能
---

# 实时私信聊天功能实现计划

## 技术栈
- **后端**: Spring WebSocket + STOMP + Redis + PostgreSQL
- **前端**: SockJS + STOMP.js

## 实现步骤

### 第一阶段：数据库设计

1. 创建消息表 `vx_messages`
   - id (BIGINT, 主键)
   - sender_id (BIGINT, 发送者)
   - receiver_id (BIGINT, 接收者)
   - content (TEXT, 消息内容)
   - message_type (VARCHAR, 消息类型: TEXT/IMAGE/VIDEO)
   - is_read (BOOLEAN, 是否已读)
   - created_at (TIMESTAMP)
   - updated_at (TIMESTAMP)

2. 创建会话表 `vx_conversations`
   - id (BIGINT, 主键)
   - user1_id (BIGINT)
   - user2_id (BIGINT)
   - last_message_id (BIGINT)
   - last_message_time (TIMESTAMP)
   - unread_count_user1 (INT)
   - unread_count_user2 (INT)
   - created_at (TIMESTAMP)
   - updated_at (TIMESTAMP)

### 第二阶段：后端实现

#### 1. 添加依赖 (pom.xml)
```xml
<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

#### 2. WebSocket 配置
- 创建 `WebSocketConfig.java`
- 配置 STOMP 端点和消息代理

#### 3. 实体类
- `Message.java` - 消息实体
- `Conversation.java` - 会话实体
- `MessageDTO.java` - 消息传输对象
- `MessageVO.java` - 消息视图对象

#### 4. Mapper 层
- `MessageMapper.java`
- `ConversationMapper.java`

#### 5. Service 层
- `ChatService.java` - 聊天业务逻辑
  - 发送消息
  - 获取历史消息
  - 标记已读
  - 获取会话列表
- `OnlineUserService.java` - 在线用户管理（Redis）

#### 6. Controller 层
- `ChatController.java` - WebSocket 消息控制器
  - `@MessageMapping("/chat.send")` - 发送消息
  - `@MessageMapping("/chat.typing")` - 正在输入状态
- `MessageController.java` - REST API 控制器
  - `GET /messages/conversations` - 获取会话列表
  - `GET /messages/history/{userId}` - 获取聊天历史
  - `PUT /messages/read/{conversationId}` - 标记已读

### 第三阶段：前端实现

#### 1. 安装依赖
```bash
npm install sockjs-client @stomp/stompjs
```

#### 2. WebSocket 服务
- 创建 `src/utils/websocket.ts`
- 封装连接、订阅、发送、断开等方法

#### 3. 聊天组件
- `ChatList.vue` - 会话列表
- `ChatWindow.vue` - 聊天窗口
- `MessageItem.vue` - 消息项
- `MessageInput.vue` - 消息输入框

#### 4. Vuex/Pinia 状态管理
- `chatStore.ts`
  - conversations (会话列表)
  - currentChat (当前聊天)
  - messages (消息列表)
  - onlineUsers (在线用户)

### 第四阶段：功能增强

1. **消息已读回执**
   - 发送已读状态
   - 显示已读/未读标识

2. **在线状态**
   - 用户上线/下线通知
   - 显示在线状态

3. **正在输入提示**
   - 实时显示对方正在输入

4. **消息类型扩展**
   - 文本消息
   - 图片消息
   - 表情包

5. **未读消息提醒**
   - 红点提示
   - 桌面通知

### 第五阶段：优化

1. **性能优化**
   - 消息分页加载
   - 虚拟滚动（长列表）
   - Redis 缓存热点数据

2. **安全性**
   - WebSocket 认证
   - 消息加密（可选）
   - 防止消息轰炸

3. **用户体验**
   - 消息发送失败重试
   - 断线重连
   - 消息本地缓存

## 关键代码示例

### WebSocket 配置
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 消息控制器
```java
@Controller
public class ChatController {
    
    @MessageMapping("/chat.send")
    @SendToUser("/queue/messages")
    public MessageVO sendMessage(MessageDTO message, Principal principal) {
        // 保存消息到数据库
        // 发送给接收者
        return chatService.sendMessage(message, principal.getName());
    }
}
```

### 前端 WebSocket 连接
```typescript
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const client = new Client({
  webSocketFactory: () => new SockJS('/api/ws'),
  connectHeaders: {
    Authorization: `Bearer ${token}`
  },
  onConnect: () => {
    // 订阅个人消息队列
    client.subscribe('/user/queue/messages', (message) => {
      handleNewMessage(JSON.parse(message.body))
    })
  }
})

client.activate()
```

## 测试计划

1. 单元测试
   - Service 层业务逻辑
   - Mapper 层数据访问

2. 集成测试
   - WebSocket 连接
   - 消息发送/接收
   - 已读状态同步

3. 压力测试
   - 并发连接数
   - 消息吞吐量

## 部署注意事项

1. **负载均衡**
   - 使用 Redis 作为消息代理
   - Session 共享

2. **监控**
   - WebSocket 连接数
   - 消息延迟
   - 错误率

3. **扩展性**
   - 考虑使用专业的消息队列（RabbitMQ/Kafka）
   - 考虑使用专业的即时通讯服务（环信/融云）
