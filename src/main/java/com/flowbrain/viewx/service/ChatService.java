package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.MessageDTO;
import com.flowbrain.viewx.pojo.vo.ConversationVO;
import com.flowbrain.viewx.pojo.vo.MessageVO;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService {

    /**
     * 发送消息
     */
    Result<MessageVO> sendMessage(Long senderId, MessageDTO messageDTO);

    /**
     * 获取聊天历史
     */
    Result<List<MessageVO>> getChatHistory(Long userId, Long otherUserId, int page, int size);

    /**
     * 获取会话列表
     */
    Result<List<ConversationVO>> getConversations(Long userId);

    /**
     * 标记消息为已读
     */
    Result<Void> markAsRead(Long userId, Long otherUserId);

    /**
     * 获取未读消息总数
     */
    Result<Integer> getTotalUnreadCount(Long userId);

    /**
     * 撤回消息（只能撤回自己发送的消息，且在2分钟内）
     */
    Result<Void> recallMessage(Long userId, Long messageId);

    /**
     * 删除消息（软删除）
     */
    Result<Void> deleteMessage(Long userId, Long messageId);
}
