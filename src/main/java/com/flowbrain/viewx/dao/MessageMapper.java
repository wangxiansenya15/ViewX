package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.Message;
import com.flowbrain.viewx.pojo.vo.MessageVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

        /**
         * 获取两个用户之间的聊天历史（过滤已删除的消息）
         */
        @Select("SELECT m.id, m.sender_id, m.receiver_id, m.content, m.message_type, " +
                        "m.is_read, m.is_recalled, m.recalled_at, m.created_at, " +
                        "u.username as sender_username, u.nickname as sender_nickname, ud.avatar_url as sender_avatar "
                        +
                        "FROM vx_messages m " +
                        "JOIN vx_users u ON m.sender_id = u.id " +
                        "LEFT JOIN vx_user_details ud ON u.id = ud.user_id " +
                        "WHERE (m.sender_id = #{userId1} AND m.receiver_id = #{userId2}) " +
                        "   OR (m.sender_id = #{userId2} AND m.receiver_id = #{userId1}) " +
                        "AND (m.is_deleted IS NULL OR m.is_deleted = false) " + // 过滤已删除的消息
                        "ORDER BY m.created_at DESC " +
                        "LIMIT #{limit} OFFSET #{offset}")
        List<MessageVO> getChatHistory(@Param("userId1") Long userId1,
                        @Param("userId2") Long userId2,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

        /**
         * 标记消息为已读
         */
        @Update("UPDATE vx_messages SET is_read = true " +
                        "WHERE receiver_id = #{userId} AND sender_id = #{otherUserId} AND is_read = false")
        int markAsRead(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

        /**
         * 获取未读消息数量
         */
        @Select("SELECT COUNT(*) FROM vx_messages " +
                        "WHERE receiver_id = #{userId} AND sender_id = #{otherUserId} AND is_read = false")
        int getUnreadCount(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

        /**
         * 获取用户的总未读消息数
         */
        @Select("SELECT COUNT(*) FROM vx_messages WHERE receiver_id = #{userId} AND is_read = false")
        int getTotalUnreadCount(@Param("userId") Long userId);

        /**
         * 撤回消息（只能撤回自己发送的消息，且在2分钟内）
         */
        @Update("UPDATE vx_messages SET is_recalled = true, recalled_at = CURRENT_TIMESTAMP " +
                        "WHERE id = #{messageId} AND sender_id = #{userId} " +
                        "AND (is_recalled IS NULL OR is_recalled = false) " +
                        "AND created_at > (CURRENT_TIMESTAMP - INTERVAL '2 minutes')")
        int recallMessage(@Param("messageId") Long messageId, @Param("userId") Long userId);

        /**
         * 删除消息（软删除，标记为已删除）
         */
        @Update("UPDATE vx_messages SET is_deleted = true " +
                        "WHERE id = #{messageId} AND (sender_id = #{userId} OR receiver_id = #{userId})")
        int deleteMessage(@Param("messageId") Long messageId, @Param("userId") Long userId);

        /**
         * 检查消息是否属于用户（发送者或接收者）
         */
        @Select("SELECT COUNT(*) > 0 FROM vx_messages " +
                        "WHERE id = #{messageId} AND (sender_id = #{userId} OR receiver_id = #{userId})")
        boolean isMessageBelongsToUser(@Param("messageId") Long messageId, @Param("userId") Long userId);

        /**
         * 检查消息是否可以撤回（是发送者且在2分钟内）
         */
        @Select("SELECT COUNT(*) > 0 FROM vx_messages " +
                        "WHERE id = #{messageId} AND sender_id = #{userId} " +
                        "AND (is_recalled IS NULL OR is_recalled = false) " +
                        "AND created_at > (CURRENT_TIMESTAMP - INTERVAL '2 minutes')")
        boolean canRecallMessage(@Param("messageId") Long messageId, @Param("userId") Long userId);
}
