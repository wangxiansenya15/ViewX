package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.dao.TopicMapper;
import com.flowbrain.viewx.dao.VideoTopicMapper;
import com.flowbrain.viewx.pojo.entity.Topic;
import com.flowbrain.viewx.pojo.entity.VideoTopic;
import com.flowbrain.viewx.service.TopicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TopicServiceImpl implements TopicService {

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private VideoTopicMapper videoTopicMapper;

    // 匹配以#开头的话题，支持中英文、数字、下划线
    private static final Pattern TOPIC_PATTERN = Pattern.compile("#([\\u4e00-\\u9fa5a-zA-Z0-9_]+)");

    @Override
    public Set<String> extractTopicsFromText(String text) {
        Set<String> topics = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return topics;
        }

        Matcher matcher = TOPIC_PATTERN.matcher(text);
        while (matcher.find()) {
            String topicName = matcher.group(1); // 获取#后面的内容
            if (!topicName.isEmpty()) {
                topics.add(topicName);
            }
        }

        log.info("从文本中提取到 {} 个话题: {}", topics.size(), topics);
        return topics;
    }

    @Override
    @Transactional
    public void associateTopicsWithVideo(Long videoId, Set<String> topicNames) {
        if (topicNames == null || topicNames.isEmpty()) {
            return;
        }

        for (String topicName : topicNames) {
            // 获取或创建话题
            Topic topic = getOrCreateTopic(topicName);

            // 创建视频-话题关联
            VideoTopic videoTopic = new VideoTopic();
            videoTopic.setVideoId(videoId);
            videoTopic.setTopicId(topic.getId());
            videoTopic.setCreatedAt(LocalDateTime.now());

            try {
                videoTopicMapper.insert(videoTopic);
                // 增加话题的视频计数
                topicMapper.incrementVideoCount(topic.getId());
                log.info("视频 {} 关联话题 {} 成功", videoId, topicName);
            } catch (Exception e) {
                // 可能是重复关联，忽略
                log.warn("视频 {} 关联话题 {} 失败（可能已存在）: {}", videoId, topicName, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public Topic getOrCreateTopic(String name) {
        // 先查找是否已存在
        Topic topic = topicMapper.findByName(name);

        if (topic == null) {
            // 不存在则创建新话题
            topic = new Topic();
            topic.setName(name);
            topic.setVideoCount(0L);
            topic.setViewCount(0L);
            topic.setCreatedAt(LocalDateTime.now());
            topic.setUpdatedAt(LocalDateTime.now());
            topic.setIsDeleted(false);

            topicMapper.insert(topic);
            log.info("创建新话题: {}", name);
        }

        return topic;
    }

    @Override
    @Transactional
    public void removeVideoTopics(Long videoId) {
        // 获取视频关联的所有话题
        List<Long> topicIds = videoTopicMapper.findTopicIdsByVideoId(videoId);

        // 删除关联
        videoTopicMapper.deleteByVideoId(videoId);

        // 减少每个话题的视频计数
        for (Long topicId : topicIds) {
            topicMapper.decrementVideoCount(topicId);
        }

        log.info("删除视频 {} 的所有话题关联，共 {} 个", videoId, topicIds.size());
    }
}
