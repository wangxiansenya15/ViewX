package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

        /**
         * 查询用户的通知列表（带关联信息）
         * 注意：使用数据库列名，MyBatis-Plus 会自动映射到实体类字段
         */
        @Select("SELECT n.*, " +
                        "u.nickname as senderNickname, " +
                        "u.avatar_url as senderAvatar, " +
                        "v.title as videoTitle " +
                        "FROM vx_notifications n " +
                        "LEFT JOIN vx_users u ON n.sender_id = u.id " +
                        "LEFT JOIN vx_videos v ON n.related_video_id = v.id " +
                        "WHERE n.recipient_id = #{userId} AND n.is_deleted = false " +
                        "ORDER BY n.created_at DESC " +
                        "LIMIT #{limit} OFFSET #{offset}")
        @Results({
                        @Result(property = "userId", column = "recipient_id"),
                        @Result(property = "type", column = "notification_type"),
                        @Result(property = "relatedVideoId", column = "related_video_id"),
                        @Result(property = "relatedCommentId", column = "related_comment_id"),
                        @Result(property = "isRead", column = "is_read"),
                        @Result(property = "createdAt", column = "created_at"),
                        @Result(property = "isDeleted", column = "is_deleted"),
                        @Result(property = "deletedAt", column = "deleted_at")
        })
        List<Notification> selectByUserId(@Param("userId") Long userId,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

        /**
         * 统计未读通知数量
         */
        @Select("SELECT COUNT(*) FROM vx_notifications " +
                        "WHERE recipient_id = #{userId} AND is_read = false AND is_deleted = false")
        long countUnread(@Param("userId") Long userId);

        /**
         * 标记所有通知为已读
         */
        @Update("UPDATE vx_notifications SET is_read = true " +
                        "WHERE recipient_id = #{userId} AND is_read = false")
        void markAllAsRead(@Param("userId") Long userId);

        /**
         * 标记单个通知为已读
         */
        @Update("UPDATE vx_notifications SET is_read = true " +
                        "WHERE id = #{notificationId} AND recipient_id = #{userId}")
        void markAsRead(@Param("notificationId") Long notificationId,
                        @Param("userId") Long userId);
}
