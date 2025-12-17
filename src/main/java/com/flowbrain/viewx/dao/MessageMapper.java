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
     * 获取两个用户之间的聊天历史
     */
    @Select("SELECT m.id, m.sender_id, m.receiver_id, m.content, m.message_type, m.is_read, m.created_at, " +
            "u.username as sender_username, u.nickname as sender_nickname, ud.avatar_url as sender_avatar " +
            "FROM vx_messages m " +
            "JOIN vx_users u ON m.sender_id = u.id " +
            "LEFT JOIN vx_user_details ud ON u.id = ud.user_id " +
            "WHERE (m.sender_id = #{userId1} AND m.receiver_id = #{userId2}) " +
            "   OR (m.sender_id = #{userId2} AND m.receiver_id = #{userId1}) " +
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
}
