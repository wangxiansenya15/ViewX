package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.UserFollow;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户关注关系 Mapper
 */
@Mapper
public interface FollowMapper {

    /**
     * 关注用户
     */
    @Insert("INSERT INTO vx_user_follows (follower_id, followed_id, created_at) " +
            "VALUES (#{followerId}, #{followedId}, NOW())")
    void insertFollow(UserFollow follow);

    /**
     * 取消关注
     */
    @Delete("DELETE FROM vx_user_follows WHERE follower_id = #{followerId} AND followed_id = #{followedId}")
    void deleteFollow(@Param("followerId") Long followerId, @Param("followedId") Long followedId);

    /**
     * 检查是否关注
     */
    @Select("SELECT COUNT(*) FROM vx_user_follows WHERE follower_id = #{followerId} AND followed_id = #{followedId}")
    int checkFollow(@Param("followerId") Long followerId, @Param("followedId") Long followedId);

    /**
     * 获取粉丝数
     */
    @Select("SELECT COUNT(*) FROM vx_user_follows WHERE followed_id = #{userId}")
    long getFollowerCount(Long userId);

    /**
     * 获取关注数
     */
    @Select("SELECT COUNT(*) FROM vx_user_follows WHERE follower_id = #{userId}")
    long getFollowingCount(Long userId);

    /**
     * 获取粉丝列表
     */
    @Select("SELECT u.id, u.username, u.nickname, ud.avatar " +
            "FROM vx_user_follows f " +
            "JOIN vx_users u ON f.follower_id = u.id " +
            "LEFT JOIN vx_user_details ud ON u.id = ud.user_id " +
            "WHERE f.followed_id = #{userId} " +
            "ORDER BY f.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<UserFollow> getFollowers(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 获取关注列表
     */
    @Select("SELECT u.id, u.username, u.nickname, ud.avatar " +
            "FROM vx_user_follows f " +
            "JOIN vx_users u ON f.followed_id = u.id " +
            "LEFT JOIN vx_user_details ud ON u.id = ud.user_id " +
            "WHERE f.follower_id = #{userId} " +
            "ORDER BY f.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<UserFollow> getFollowing(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
}
