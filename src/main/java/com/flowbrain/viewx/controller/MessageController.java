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
}
