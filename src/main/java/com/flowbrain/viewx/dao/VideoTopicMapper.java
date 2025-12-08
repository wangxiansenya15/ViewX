package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.VideoTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoTopicMapper extends BaseMapper<VideoTopic> {

    /**
     * 根据视频ID查找所有关联的话题ID
     */
    @Select("SELECT topic_id FROM vx_video_topics WHERE video_id = #{videoId}")
    List<Long> findTopicIdsByVideoId(@Param("videoId") Long videoId);

    /**
     * 根据话题ID查找所有关联的视频ID
     */
    @Select("SELECT video_id FROM vx_video_topics WHERE topic_id = #{topicId}")
    List<Long> findVideoIdsByTopicId(@Param("topicId") Long topicId);

    /**
     * 删除视频的所有话题关联
     */
    @Select("DELETE FROM vx_video_topics WHERE video_id = #{videoId}")
    void deleteByVideoId(@Param("videoId") Long videoId);
}
