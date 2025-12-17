# Social Chat and Notification System Implementation Guide

## Overview
This document describes the implementation of a complete social chat and notification system with RabbitMQ integration for ViewX platform.

## Architecture

### Components
1. **RabbitMQ Message Queue** - Asynchronous notification processing
2. **WebSocket** - Real-time notification delivery
3. **Notification System** - Like, Comment, Follow, Favorite notifications
4. **Chat System** - Mutual-follow based private messaging

## Features Implemented

### 1. Notification System with RabbitMQ

#### Producer Service (`NotificationProducerService`)
- Sends notification messages to RabbitMQ queues
- Supports: Like, Comment, Follow, Favorite notifications
- Prevents self-notifications
- Async processing for better performance

#### Consumer Service (`NotificationConsumerService`)
- Consumes messages from RabbitMQ
- Persists notifications to database
- Sends real-time updates via WebSocket to `/topic/notifications/{userId}`
- Automatic retry on failure

#### Integration Points
Modified `InteractionServiceImpl` to trigger notifications on:
- **Like**: When user likes a video
- **Comment**: When user comments on a video  
- **Follow**: When user follows another user
- **Favorite**: When user favorites a video

### 2. Chat System

#### Prerequisites for Chat
- Users must mutually follow each other to chat
- Check via `InteractionService.isMutualFollow(userId1, userId2)`

#### Chat Service (`ChatService`)
Methods:
- `sendMessage(senderId, messageDTO)` - Send a chat message
- `getChatHistory(userId, otherUserId, page, size)` - Get conversation history
- `getConversations(userId)` - Get all conversations for a user
- `markAsRead(userId, otherUserId)` - Mark messages as read
- `getTotalUnreadCount(userId)` - Get total unread message count

#### WebSocket Chat Controller (`ChatWebSocketController`)
- Endpoint: `/app/chat.send`
- Subscription: `/topic/chat/{userId}`
- Real-time message delivery

### 3. RabbitMQ Configuration

#### Exchanges
- `viewx.exchange` - Main direct exchange for notifications
- `viewx.dlx.exchange` - Dead letter exchange for failed messages

#### Queues
- `viewx.notification` - Notification queue with dead letter support
- `viewx.notification.dlq` - Dead letter queue for failed notifications

#### Message Flow
```
User Action → NotificationProducerService → RabbitMQ Queue → NotificationConsumerService → Database + WebSocket
```

## Database Schema

### Notifications Table (vx_notifications)
```sql
CREATE TABLE vx_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    related_video_id BIGINT,
    related_comment_id BIGINT,
    content TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    INDEX idx_recipient (recipient_id, is_read, created_at)
);
```

### Messages Table (vx_messages)
```sql
CREATE TABLE vx_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id, created_at),
    INDEX idx_receiver_unread (receiver_id, is_read)
);
```

### Conversations Table (vx_conversations)
```sql
CREATE TABLE vx_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    last_message_id BIGINT,
    last_message_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users (LEAST(user1_id, user2_id), GREATEST(user1_id, user2_id))
);
```

## Frontend Integration

### WebSocket Connection
```typescript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    // Subscribe to notifications
    stompClient.subscribe(`/topic/notifications/${userId}`, (message) => {
        const notification = JSON.parse(message.body);
        // Handle notification (show toast, update badge, etc.)
    });
    
    // Subscribe to chat messages
    stompClient.subscribe(`/topic/chat/${userId}`, (message) => {
        const chatMessage = JSON.parse(message.body);
        // Handle chat message
    });
});
```

### Send Chat Message
```typescript
stompClient.send('/app/chat.send', {}, JSON.stringify({
    receiverId: otherUserId,
    content: messageText
}));
```

### API Endpoints

#### Notifications
- `GET /api/notifications` - Get notification list
- `GET /api/notifications/unread-count` - Get unread count
- `PUT /api/notifications/{id}/read` - Mark as read
- `PUT /api/notifications/read-all` - Mark all as read
- `DELETE /api/notifications/{id}` - Delete notification

#### Chat
- `POST /api/chat/send` - Send message (REST fallback)
- `GET /api/chat/history/{otherUserId}` - Get chat history
- `GET /api/chat/conversations` - Get conversation list
- `PUT /api/chat/read/{otherUserId}` - Mark conversation as read
- `GET /api/chat/unread-count` - Get total unread count

## Configuration

### application.yml
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        acknowledge-mode: auto
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          multiplier: 2
```

## Testing

### Test Notification Flow
1. User A likes User B's video
2. `NotificationProducerService` sends message to RabbitMQ
3. `NotificationConsumerService` receives message
4. Notification saved to database
5. WebSocket message sent to User B
6. User B sees real-time notification

### Test Chat Flow
1. Verify User A and User B mutually follow each other
2. User A sends message via WebSocket
3. Message saved to database
4. User B receives message via WebSocket subscription
5. Unread count updated

## Performance Considerations

1. **RabbitMQ** - Async processing prevents blocking user actions
2. **WebSocket** - Efficient real-time delivery without polling
3. **Database Indexes** - Optimized for common queries
4. **Dead Letter Queue** - Failed messages don't block the system
5. **Message TTL** - Old messages auto-expire (notifications: 24h, chat: 7d)

## Security

1. **Authentication** - All endpoints require valid JWT token
2. **Authorization** - Users can only access their own data
3. **Mutual Follow Check** - Chat restricted to mutual followers
4. **Input Validation** - All user input sanitized
5. **Rate Limiting** - Prevent spam (recommended to add)

## Monitoring

### Key Metrics to Monitor
- RabbitMQ queue depth
- Message processing rate
- Failed message count (DLQ)
- WebSocket connection count
- Notification delivery latency

### Logging
- All notification sends logged
- Failed message processing logged with stack trace
- WebSocket connection/disconnection events logged

## Future Enhancements

1. **Push Notifications** - Mobile push via FCM/APNS
2. **Email Notifications** - Digest emails for inactive users
3. **Message Reactions** - Emoji reactions to chat messages
4. **Typing Indicators** - Show when other user is typing
5. **Read Receipts** - Show when messages are read
6. **Group Chat** - Multi-user conversations
7. **File Sharing** - Send images/videos in chat
8. **Message Search** - Full-text search in conversations

## Troubleshooting

### Notifications Not Received
1. Check RabbitMQ is running: `rabbitmqctl status`
2. Check queue has consumers: `rabbitmqctl list_queues`
3. Verify WebSocket connection established
4. Check user subscribed to correct topic

### Chat Messages Not Delivered
1. Verify mutual follow relationship
2. Check WebSocket connection
3. Verify conversation exists in database
4. Check message saved to database

### High Memory Usage
1. Check DLQ for stuck messages
2. Verify message TTL configured
3. Monitor RabbitMQ memory usage
4. Consider increasing consumer count

## Deployment Checklist

- [ ] RabbitMQ server installed and running
- [ ] Database migrations applied
- [ ] Environment variables configured
- [ ] WebSocket CORS configured for production domain
- [ ] Monitoring/alerting set up
- [ ] Load testing completed
- [ ] Backup strategy for messages
- [ ] Rate limiting configured

## Support

For issues or questions:
1. Check application logs
2. Check RabbitMQ management console (http://localhost:15672)
3. Review this documentation
4. Contact development team
