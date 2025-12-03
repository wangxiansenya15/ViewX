package com.flowbrain.viewx.dao;

import org.apache.ibatis.annotations.*;

@Mapper
public interface InteractionMapper {

    // --- Likes ---
    @Insert("INSERT INTO vx_video_likes (user_id, video_id) VALUES (#{userId}, #{videoId})")
    void insertLike(@Param("userId") Long userId, @Param("videoId") Long videoId);

    @Delete("DELETE FROM vx_video_likes WHERE user_id = #{userId} AND video_id = #{videoId}")
    void deleteLike(@Param("userId") Long userId, @Param("videoId") Long videoId);

    @Select("SELECT COUNT(*) FROM vx_video_likes WHERE user_id = #{userId} AND video_id = #{videoId}")
    int checkLike(@Param("userId") Long userId, @Param("videoId") Long videoId);

    @Update("UPDATE vx_videos SET like_count = like_count + 1 WHERE id = #{videoId}")
    void incrementVideoLikeCount(Long videoId);

    @Update("UPDATE vx_videos SET like_count = like_count - 1 WHERE id = #{videoId}")
    void decrementVideoLikeCount(Long videoId);

    // --- Favorites ---
    @Insert("INSERT INTO vx_video_favorites (user_id, video_id) VALUES (#{userId}, #{videoId})")
    void insertFavorite(@Param("userId") Long userId, @Param("videoId") Long videoId);

    @Delete("DELETE FROM vx_video_favorites WHERE user_id = #{userId} AND video_id = #{videoId}")
    void deleteFavorite(@Param("userId") Long userId, @Param("videoId") Long videoId);

    @Select("SELECT COUNT(*) FROM vx_video_favorites WHERE user_id = #{userId} AND video_id = #{videoId}")
    int checkFavorite(@Param("userId") Long userId, @Param("videoId") Long videoId);
    
    // Note: Video table doesn't have favorite_count in the schema provided earlier, 
    // but usually we might want to track it. Assuming we only track likes/views/comments for now based on schema.
    // If needed, we can add favorite_count to vx_videos later.
}
