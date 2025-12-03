package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.VideoComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("INSERT INTO vx_video_comments (id, video_id, user_id, parent_id, content) " +
            "VALUES (#{id}, #{videoId}, #{userId}, #{parentId}, #{content})")
    void insertComment(VideoComment comment);

    @Delete("DELETE FROM vx_video_comments WHERE id = #{id}")
    void deleteComment(Long id);

    @Select("SELECT c.*, u.nickname as userNickname, u.avatar_url as userAvatar " + // Assuming avatar_url is in user_details, might need join
            "FROM vx_video_comments c " +
            "LEFT JOIN vx_users u ON c.user_id = u.id " + // Simplified, usually join user_details for avatar
            "WHERE c.video_id = #{videoId} AND c.parent_id IS NULL " +
            "ORDER BY c.created_at DESC")
    List<VideoComment> selectRootComments(Long videoId);

    @Select("SELECT c.*, u.nickname as userNickname " +
            "FROM vx_video_comments c " +
            "LEFT JOIN vx_users u ON c.user_id = u.id " +
            "WHERE c.parent_id = #{parentId} " +
            "ORDER BY c.created_at ASC")
    List<VideoComment> selectReplies(Long parentId);

    @Update("UPDATE vx_videos SET comment_count = comment_count + 1 WHERE id = #{videoId}")
    void incrementVideoCommentCount(Long videoId);
}
