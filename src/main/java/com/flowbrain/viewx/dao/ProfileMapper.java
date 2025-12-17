package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户资料 Mapper
 * 用途：处理用户资料相关的数据库操作
 */
@Mapper
public interface ProfileMapper extends BaseMapper<User> {

    /**
     * 获取用户的统计信息（粉丝数、关注数等）
     */
    @Select("SELECT COUNT(*) FROM vx_user_follows WHERE followed_id = #{userId}")
    Integer countFollowers(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM vx_user_follows WHERE follower_id = #{userId}")
    Integer countFollowing(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM vx_videos WHERE uploader_id = #{userId} AND is_deleted = false")
    Integer countVideos(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(like_count), 0) FROM vx_videos WHERE uploader_id = #{userId} AND is_deleted = false")
    Integer countTotalLikes(@Param("userId") Long userId);
}
