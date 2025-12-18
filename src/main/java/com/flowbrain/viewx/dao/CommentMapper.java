package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.VideoComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<VideoComment> {

        @Insert("INSERT INTO vx_video_comments (id, video_id, user_id, parent_id, content) " +
                        "VALUES (#{id}, #{videoId}, #{userId}, #{parentId}, #{content})")
        void insertComment(VideoComment comment);

        @Delete("DELETE FROM vx_video_comments WHERE id = #{id}")
        void deleteComment(Long id);

        @Select("SELECT c.id, c.video_id, c.user_id, c.parent_id, c.content, c.created_at, c.like_count, " +
                        "u.username, u.nickname, ud.avatar_url as avatar " +
                        "FROM vx_video_comments c " +
                        "LEFT JOIN vx_users u ON c.user_id = u.id " +
                        "LEFT JOIN vx_user_details ud ON c.user_id = ud.user_id " +
                        "WHERE c.video_id = #{videoId} AND c.parent_id IS NULL " +
                        "ORDER BY c.created_at DESC")
        @Results(id = "commentResultMap", value = {
                        @Result(property = "id", column = "id"),
                        @Result(property = "videoId", column = "video_id"),
                        @Result(property = "userId", column = "user_id"),
                        @Result(property = "parentId", column = "parent_id"),
                        @Result(property = "content", column = "content"),
                        @Result(property = "createdAt", column = "created_at"),
                        @Result(property = "likeCount", column = "like_count"),
                        @Result(property = "username", column = "username"),
                        @Result(property = "nickname", column = "nickname"),
                        @Result(property = "avatar", column = "avatar")
        })
        List<VideoComment> selectRootComments(Long videoId);

        @Select("SELECT c.id, c.video_id, c.user_id, c.parent_id, c.content, c.created_at, c.like_count, " +
                        "u.username, u.nickname, ud.avatar_url as avatar " +
                        "FROM vx_video_comments c " +
                        "LEFT JOIN vx_users u ON c.user_id = u.id " +
                        "LEFT JOIN vx_user_details ud ON c.user_id = ud.user_id " +
                        "WHERE c.parent_id = #{parentId} " +
                        "ORDER BY c.created_at ASC")
        @ResultMap("commentResultMap")
        List<VideoComment> selectReplies(Long parentId);

        @Update("UPDATE vx_videos SET comment_count = comment_count + 1 WHERE id = #{videoId}")
        void incrementVideoCommentCount(Long videoId);
}
