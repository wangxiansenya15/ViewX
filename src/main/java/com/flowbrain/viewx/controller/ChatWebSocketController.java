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
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageDTO messageDTO, Principal principal) {
        try {
            String username = principal.getName();
            Long senderId = userService.getUserByUsername(username).getId();

            // 保存消息
            Result<MessageVO> result = chatService.sendMessage(senderId, messageDTO);

            if (result.getCode() == 200) {
                MessageVO messageVO = result.getData();

                // 发送给接收者
                messagingTemplate.convertAndSendToUser(
                        messageDTO.getReceiverId().toString(),
                        "/queue/messages",
                        messageVO);

                // 发送给发送者（确认）
                messagingTemplate.convertAndSendToUser(
                        senderId.toString(),
                        "/queue/messages",
                        messageVO);

                log.info("消息已通过 WebSocket 发送: {} -> {}", senderId, messageDTO.getReceiverId());
            }
        } catch (Exception e) {
            log.error("WebSocket 发送消息失败", e);
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

            // 通知接收者发送者正在输入
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/typing",
                    senderId);
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
