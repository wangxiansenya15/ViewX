package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.MessageDTO;
import com.flowbrain.viewx.pojo.vo.MessageVO;
import com.flowbrain.viewx.service.ChatService;
import com.flowbrain.viewx.service.UserService;
import com.flowbrain.viewx.service.impl.ChatServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 聊天控制器
 */
@Slf4j
@Controller
public class ChatWebSocketController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatServiceImpl chatServiceImpl;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 处理发送消息请求
     * 客户端发送到: /app/chat.send
     */
    @Autowired
    private SimpUserRegistry userRegistry;

    // 手动注入 ObjectMapper 用于序列化
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    {
        // 注册 JavaTimeModule 以支持 LocalDateTime
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        // 禁用日期写为时间戳
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 关键修复：将 Long 类型序列化为 String，防止前端 JavaScript 精度丢失
        com.fasterxml.jackson.databind.module.SimpleModule simpleModule = new com.fasterxml.jackson.databind.module.SimpleModule();
        simpleModule.addSerializer(Long.class, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageDTO messageDTO, Principal principal) {
        try {
            String username = principal.getName();
            log.info("=== 开始处理消息发送 ===");

            // 打印所有在线用户，帮助调试
            log.info("当前在线用户数: {}", userRegistry.getUserCount());
            userRegistry.getUsers().forEach(user -> {
                log.info("- 在线用户: {}", user.getName());
            });

            log.info("发送者用户名: {}", username);
            log.info("接收者ID: {}", messageDTO.getReceiverId());
            log.info("消息内容: {}", messageDTO.getContent());

            // 获取发送者ID
            Long senderId = userService.getUserByUsername(username).getId();
            log.info("发送者ID: {}", senderId);

            // 保存消息
            Result<MessageVO> result = chatService.sendMessage(senderId, messageDTO);
            log.info("消息保存结果: code={}, message={}", result.getCode(), result.getMessage());

            if (result.getCode() == Result.OK) {
                MessageVO messageVO = result.getData();
                log.info("消息VO: {}", messageVO);

                // 手动序列化为 JSON 字符串，避免 Spring Messaging 转换器问题
                String messageJson = objectMapper.writeValueAsString(messageVO);
                log.info("序列化后的消息: {}", messageJson);

                // 获取接收者的用户名
                Result<com.flowbrain.viewx.pojo.entity.User> receiverResult = userService
                        .getUserById(messageDTO.getReceiverId());
                log.info("接收者查询结果: code={}, message={}", receiverResult.getCode(), receiverResult.getMessage());

                com.flowbrain.viewx.pojo.entity.User receiver = receiverResult.getData();
                if (receiver != null) {
                    log.info("接收者用户名: {}", receiver.getUsername());

                    // 发送给接收者
                    messagingTemplate.convertAndSendToUser(
                            receiver.getUsername(), // 使用用户名，不是用户ID
                            "/queue/messages",
                            messageJson); // 发送 JSON 字符串

                    log.info("✅ 消息已发送给接收者: {} (用户名: {})", messageDTO.getReceiverId(), receiver.getUsername());
                } else {
                    log.error("❌ 接收者为 null，无法发送消息");
                }

                // 发送给发送者（确认）
                messagingTemplate.convertAndSendToUser(
                        username, // 使用当前用户的用户名
                        "/queue/messages",
                        messageJson); // 发送 JSON 字符串

                log.info("✅ 消息确认已发送给发送者: {}", username);
                log.info("=== 消息发送处理完成 ===");
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
                        "/queue/errors", // 使用专门的错误队列
                        errorJson);

                log.info("✅ 错误消息已发送给发送者: {}", username);
            }
        } catch (Exception e) {
            log.error("❌ WebSocket 发送消息失败", e);

            // 尝试发送通用错误消息给用户
            try {
                String username = principal.getName();
                java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("type", "error");
                errorResponse.put("code", Result.SERVER_ERROR);
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
    }

    /**
     * 处理正在输入状态
     * 客户端发送到: /app/chat.typing
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload Long receiverId, Principal principal) {
        try {
            String username = principal.getName();
            Long senderId = userService.getUserByUsername(username).getId();

            // 获取接收者的用户名
            com.flowbrain.viewx.pojo.entity.User receiver = userService.getUserById(receiverId).getData();
            if (receiver != null) {
                // 通知接收者发送者正在输入
                messagingTemplate.convertAndSendToUser(
                        receiver.getUsername(), // 使用用户名，不是用户ID
                        "/queue/typing",
                        senderId);

                log.debug("正在输入状态已发送: {} -> {}", username, receiver.getUsername());
            }
        } catch (Exception e) {
            log.error("发送正在输入状态失败", e);
        }
    }

    /**
     * 用户连接时的处理
     */
    @MessageMapping("/chat.connect")
    @SendToUser("/queue/connect")
    public String connect(Principal principal) {
        try {
            String username = principal.getName();
            Long userId = userService.getUserByUsername(username).getId();

            // 设置用户在线状态
            chatServiceImpl.setUserOnline(userId);

            log.info("用户上线: {}", username);
            return "connected";
        } catch (Exception e) {
            log.error("处理用户连接失败", e);
            return "error";
        }
    }
}
