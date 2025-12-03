package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {

    @Select("SELECT * FROM vx_videos WHERE id = #{id}")
    Video selectById(Long id);

    @Select("SELECT * FROM vx_videos WHERE status = 'APPROVED' ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Video> selectLatestVideos(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM vx_videos WHERE status = 'APPROVED' ORDER BY content_embedding <-> #{embedding} LIMIT #{limit}")
    List<Video> selectByVector(@Param("embedding") String embeddingVector, @Param("limit") int limit);

    @Select("SELECT * FROM vx_videos WHERE status = 'APPROVED' AND id IN (SELECT video_id FROM video_tags WHERE tag IN (#{tags}))")
    List<Video> selectByTags(@Param("tags") List<String> tags, @Param("offset") int offset, @Param("limit") int limit);
    
    // For recommendation, we might need a more complex query or just fetch candidates
    @Select("SELECT * FROM vx_videos WHERE status = 'APPROVED' LIMIT 100") 
    List<Video> selectCandidateVideos();
}
