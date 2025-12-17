package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.Conversation;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

/**
 * 会话 Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 获取或创建会话
     */
    @Select("SELECT * FROM vx_conversations " +
            "WHERE (user1_id = #{user1Id} AND user2_id = #{user2Id}) " +
            "   OR (user1_id = #{user2Id} AND user2_id = #{user1Id})")
    Conversation getConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    /**
     * 更新会话的最后消息信息
     */
    @Update("UPDATE vx_conversations SET " +
            "last_message_id = #{messageId}, " +
            "last_message_time = #{messageTime}, " +
            "unread_count_user1 = CASE WHEN user1_id = #{receiverId} THEN unread_count_user1 + 1 ELSE unread_count_user1 END, "
            +
            "unread_count_user2 = CASE WHEN user2_id = #{receiverId} THEN unread_count_user2 + 1 ELSE unread_count_user2 END "
            +
            "WHERE id = #{conversationId}")
    int updateLastMessage(@Param("conversationId") Long conversationId,
            @Param("messageId") Long messageId,
            @Param("messageTime") LocalDateTime messageTime,
            @Param("receiverId") Long receiverId);

    /**
     * 清空未读计数
     */
    @Update("UPDATE vx_conversations SET " +
            "unread_count_user1 = CASE WHEN user1_id = #{userId} THEN 0 ELSE unread_count_user1 END, " +
            "unread_count_user2 = CASE WHEN user2_id = #{userId} THEN 0 ELSE unread_count_user2 END " +
            "WHERE (user1_id = #{userId} AND user2_id = #{otherUserId}) " +
            "   OR (user1_id = #{otherUserId} AND user2_id = #{userId})")
    int clearUnreadCount(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
