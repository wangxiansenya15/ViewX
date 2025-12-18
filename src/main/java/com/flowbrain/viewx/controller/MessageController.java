package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.vo.ConversationVO;
import com.flowbrain.viewx.pojo.vo.MessageVO;
import com.flowbrain.viewx.service.ChatService;
import com.flowbrain.viewx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.flowbrain.viewx.dao.MessageMapper messageMapper;

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserByUsername(username).getId();
            return chatService.getConversations(userId);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return Result.serverError("获取会话列表失败");
        }
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history/{otherUserId}")
    public Result<List<MessageVO>> getChatHistory(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserByUsername(username).getId();
            return chatService.getChatHistory(userId, otherUserId, page, size);
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            return Result.serverError("获取聊天历史失败");
        }
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{otherUserId}")
    public Result<Void> markAsRead(
            @PathVariable Long otherUserId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserByUsername(username).getId();
            return chatService.markAsRead(userId, otherUserId);
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return Result.serverError("标记已读失败");
        }
    }

    /**
     * 获取未读消息总数
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserByUsername(username).getId();
            return chatService.getTotalUnreadCount(userId);
        } catch (Exception e) {
            log.error("获取未读消息数失败", e);
            return Result.serverError("获取未读消息数失败");
        }
    }

    /**
     * 撤回消息
     * PUT /messages/{messageId}/recall
     */
    @PutMapping("/{messageId}/recall")
    public Result<Void> recallMessage(
            @PathVariable Long messageId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserByUsername(username).getId();

            log.info("🔄 用户 {} (ID: {}) 尝试撤回消息: {}", username, userId, messageId);

            // 先获取消息以确定接收者
            com.flowbrain.viewx.pojo.entity.Message message = messageMapper.selectById(messageId);

            if (message == null) {
                log.error("❌ 消息不存在: messageId={}", messageId);
                return Result.notFound("消息不存在");
            }

            log.info("📨 消息详情: senderId={}, receiverId={}, content={}",
                    message.getSenderId(), message.getReceiverId(),
                    message.getContent().substring(0, Math.min(20, message.getContent().length())));

            Result<Void> result = chatService.recallMessage(userId, messageId);

            if (result.getCode() == 200) {
                log.info("✅ 消息撤回成功: messageId={}, userId={}", messageId, userId);

                // 发送 WebSocket 通知
                MessageActionNotification notificationObj = new MessageActionNotification(messageId, userId,
                        "MESSAGE_RECALLED");
                String notification = objectMapper.writeValueAsString(notificationObj);

                log.info("📤 准备发送 WebSocket 通知: {}", notification);

                // 通知发送者（如果是多端同步的话很有用）
                log.info("📤 发送撤回通知给发送者: {}", username);
                messagingTemplate.convertAndSendToUser(username, "/queue/recall", notification);

                // 通知接收者
                String receiverUsername = userService.getUsernameById(message.getReceiverId());
                log.info("🔍 接收者用户名: {}", receiverUsername);

                if (receiverUsername != null && !receiverUsername.equals(username)) {
                    log.info("📤 发送撤回通知给接收者: {}", receiverUsername);
                    messagingTemplate.convertAndSendToUser(receiverUsername, "/queue/recall", notification);
                    log.info("✅ 已成功发送撤回通知给: {}", receiverUsername);
                } else if (receiverUsername == null) {
                    log.warn("⚠️ 接收者用户名为空，无法发送通知");
                } else {
                    log.info("ℹ️ 发送者和接收者是同一人，跳过重复通知");
                }
            } else {
                log.warn("❌ 消息撤回失败: {}", result.getMessage());
            }

            return result;
        } catch (Exception e) {
            log.error("❌ 撤回消息失败", e);
            return Result.serverError("撤回消息失败: " + e.getMessage());
        }
    }

    /**
     * 删除消息
     * DELETE /messages/{messageId}
     */
    @DeleteMapping("/{messageId}")
    public Result<Void> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserByUsername(username).getId();

            log.info("用户 {} 尝试删除消息: {}", username, messageId);

            // 获取消息详情
            com.flowbrain.viewx.pojo.entity.Message message = messageMapper.selectById(messageId);

            Result<Void> result = chatService.deleteMessage(userId, messageId);

            if (result.getCode() == 200 && message != null) {
                log.info("✅ 消息删除成功: messageId={}, userId={}", messageId, userId);

                // 发送 WebSocket 通知
                String notification = objectMapper.writeValueAsString(
                        new MessageActionNotification(messageId, userId, "MESSAGE_DELETED"));

                // 通知发送者
                messagingTemplate.convertAndSendToUser(username, "/queue/delete", notification);

                // 如果是发送者删除，且这是全局删除，则通知接收者
                Long receiverId = message.getSenderId().equals(userId) ? message.getReceiverId()
                        : message.getSenderId();

                String otherUsername = userService.getUsernameById(receiverId);
                if (otherUsername != null) {
                    messagingTemplate.convertAndSendToUser(otherUsername, "/queue/delete", notification);
                    log.info("已发送删除通知给: {}", otherUsername);
                }
            } else {
                log.warn("❌ 消息删除失败: {}", result.getMessage());
            }

            return result;
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return Result.serverError("删除消息失败");
        }
    }

    /**
     * 消息操作通知内部类
     */
    private static class MessageActionNotification {
        public Long messageId;
        public Long userId;
        public String type;

        public MessageActionNotification(Long messageId, Long userId, String type) {
            this.messageId = messageId;
            this.userId = userId;
            this.type = type;
        }
    }
}
