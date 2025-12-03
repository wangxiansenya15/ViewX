package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO vx_notifications (id, recipient_id, sender_id, notification_type, related_video_id, related_comment_id, content, is_read) " +
            "VALUES (#{id}, #{recipientId}, #{senderId}, #{notificationType}, #{relatedVideoId}, #{relatedCommentId}, #{content}, false)")
    void insert(Notification notification);

    @Select("SELECT n.*, u.nickname as senderNickname, u.avatar_url as senderAvatar, v.title as videoTitle " +
            "FROM vx_notifications n " +
            "LEFT JOIN vx_users u ON n.sender_id = u.id " +
            "LEFT JOIN vx_videos v ON n.related_video_id = v.id " +
            "WHERE n.recipient_id = #{userId} " +
            "ORDER BY n.created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Notification> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM vx_notifications WHERE recipient_id = #{userId} AND is_read = false")
    long countUnread(@Param("userId") Long userId);

    @Update("UPDATE vx_notifications SET is_read = true WHERE recipient_id = #{userId} AND is_read = false")
    void markAllAsRead(@Param("userId") Long userId);
}
