package com.flowbrain.viewx.service;

import com.flowbrain.viewx.pojo.entity.Topic;

import java.util.Set;

public interface TopicService {

    /**
     * 从文本中提取话题（以#开头的词）
     * 
     * @param text 文本内容
     * @return 话题名称集合（不含#）
     */
    Set<String> extractTopicsFromText(String text);

    /**
     * 为视频关联话题
     * 
     * @param videoId    视频ID
     * @param topicNames 话题名称集合（不含#）
     */
    void associateTopicsWithVideo(Long videoId, Set<String> topicNames);

    /**
     * 根据名称获取或创建话题
     * 
     * @param name 话题名称（不含#）
     * @return 话题实体
     */
    Topic getOrCreateTopic(String name);

    /**
     * 删除视频的所有话题关联
     * 
     * @param videoId 视频ID
     */
    void removeVideoTopics(Long videoId);
}
