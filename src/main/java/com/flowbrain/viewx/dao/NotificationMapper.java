package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.Notification;
import com.flowbrain.viewx.pojo.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

        /**
         * 查询用户的通知列表 (带详细信息)
         */
        @Select({
                        "<script>",
                        "SELECT ",
                        "  n.id, n.notification_type, n.sender_id, n.related_video_id, ",
                        "  n.related_comment_id, n.content, n.is_read, n.created_at,",
                        "  u.username AS senderUsername, u.nickname AS senderNickname, ud.avatar_url AS senderAvatar,",
                        "  v.title AS relatedVideoTitle, v.cover_url AS relatedVideoCover,",
                        "  c.content AS relatedCommentContent",
                        "FROM vx_notifications n",
                        "LEFT JOIN vx_users u ON n.sender_id = u.id",
                        "LEFT JOIN vx_user_details ud ON u.id = ud.user_id",
                        "LEFT JOIN vx_videos v ON n.related_video_id = v.id",
                        "LEFT JOIN vx_video_comments c ON n.related_comment_id = c.id",
                        "WHERE n.recipient_id = #{recipientId} AND n.is_deleted = FALSE",
                        "<if test='notificationType != null'>",
                        "  AND n.notification_type = #{notificationType}",
                        "</if>",
                        "<if test='unreadOnly != null and unreadOnly == true'>",
                        "  AND n.is_read = FALSE",
                        "</if>",
                        "ORDER BY n.created_at DESC",
                        "LIMIT #{limit} OFFSET #{offset}",
                        "</script>"
        })
        List<NotificationVO> selectNotificationList(
                        @Param("recipientId") Long recipientId,
                        @Param("notificationType") String notificationType,
                        @Param("unreadOnly") Boolean unreadOnly,
                        @Param("limit") Integer limit,
                        @Param("offset") Integer offset);

        /**
         * 获取未读通知数量
         */
        @Select("SELECT COUNT(*) FROM vx_notifications WHERE recipient_id = #{recipientId} AND is_read = FALSE AND is_deleted = FALSE")
        Long countUnread(@Param("recipientId") Long recipientId);

        /**
         * 标记所有通知为已读
         */
        @Update("UPDATE vx_notifications SET is_read = TRUE WHERE recipient_id = #{recipientId} AND is_read = FALSE AND is_deleted = FALSE")
        int markAllAsRead(@Param("recipientId") Long recipientId);
}
