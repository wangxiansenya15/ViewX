package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {

    /**
     * 根据话题名称查找话题
     */
    @Select("SELECT * FROM vx_topics WHERE name = #{name} AND is_deleted = false")
    Topic findByName(@Param("name") String name);

    /**
     * 增加话题的视频计数
     */
    @Select("UPDATE vx_topics SET video_count = video_count + 1, updated_at = NOW() WHERE id = #{topicId}")
    void incrementVideoCount(@Param("topicId") Long topicId);

    /**
     * 减少话题的视频计数
     */
    @Select("UPDATE vx_topics SET video_count = video_count - 1, updated_at = NOW() WHERE id = #{topicId} AND video_count > 0")
    void decrementVideoCount(@Param("topicId") Long topicId);
}
