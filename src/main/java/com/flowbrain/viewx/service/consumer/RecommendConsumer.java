package com.flowbrain.viewx.service.consumer;

import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.common.EventType;
import com.flowbrain.viewx.common.RedisKeyConstants;
import com.flowbrain.viewx.pojo.dto.BaseEvent;
import com.flowbrain.viewx.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 推荐算法更新消费者
 * 功能：根据用户行为实时更新推荐算法的数据
 */
@Service
@Slf4j
public class RecommendConsumer {

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_RECOMMEND_UPDATE, concurrency = "3-10")
    public void handleRecommendUpdate(BaseEvent event) {
        try {
            log.info("接收到推荐更新事件: eventId={}, eventType={}", event.getEventId(), event.getEventType());

            String eventType = event.getEventType();
            Long userId = event.getUserId();
            Long videoId = (Long) event.getData().get("videoId");

            // 幂等性检查（防止重复消费）- 使用标准 Key
            String idempotentKey = RedisKeyConstants.Recommend.getProcessedEventKey(event.getEventId());
            Boolean isProcessed = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 5, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(isProcessed)) {
                log.warn("事件已处理，跳过: eventId={}", event.getEventId());
                return; // Spring AMQP 会自动 ACK
            }

            // 根据事件类型更新推荐数据
            switch (eventType) {
                case EventType.VIDEO_PLAY:
                    handleVideoPlay(userId, videoId, event);
                    break;
                case EventType.VIDEO_LIKE:
                    handleVideoLike(userId, videoId);
                    break;
                case EventType.VIDEO_UNLIKE:
                    handleVideoUnlike(userId, videoId);
                    break;
                case EventType.VIDEO_FAVORITE:
                    handleVideoFavorite(userId, videoId);
                    break;
                case EventType.VIDEO_SHARE:
                    handleVideoShare(userId, videoId);
                    break;
                default:
                    log.warn("未处理的事件类型: {}", eventType);
            }

            // 更新视频热度分数
            recommendService.updateVideoScore(videoId);

            log.info("推荐数据更新成功: eventId={}", event.getEventId());
            // Spring AMQP 自动 ACK

        } catch (Exception e) {
            log.error("推荐更新失败: eventId={}", event.getEventId(), e);
            throw new RuntimeException("推荐更新失败", e);
        }
    }

    /**
     * 处理视频播放事件
     */
    private void handleVideoPlay(Long userId, Long videoId, BaseEvent event) {
        Integer watchDuration = (Integer) event.getData().get("watchDuration");

        // 更新用户观看历史 - 使用标准 Key
        String historyKey = RedisKeyConstants.Recommend.getWatchHistoryKey(userId);
        redisTemplate.opsForZSet().add(historyKey, videoId, System.currentTimeMillis());

        // 如果观看时长超过30秒，认为是有效观看
        if (watchDuration != null && watchDuration > 30) {
            String interestKey = RedisKeyConstants.Recommend.getUserInterestKey(userId);
            redisTemplate.opsForZSet().incrementScore(interestKey, "video:" + videoId, 1.0);
        }

        log.info("更新用户观看历史: userId={}, videoId={}, duration={}", userId, videoId, watchDuration);
    }

    /**
     * 处理点赞事件
     */
    private void handleVideoLike(Long userId, Long videoId) {
        String interestKey = RedisKeyConstants.Recommend.getUserInterestKey(userId);
        redisTemplate.opsForZSet().incrementScore(interestKey, "video:" + videoId, 3.0);
        log.info("更新用户兴趣（点赞）: userId={}, videoId={}", userId, videoId);
    }

    /**
     * 处理取消点赞事件
     */
    private void handleVideoUnlike(Long userId, Long videoId) {
        String interestKey = RedisKeyConstants.Recommend.getUserInterestKey(userId);
        redisTemplate.opsForZSet().incrementScore(interestKey, "video:" + videoId, -3.0);
        log.info("更新用户兴趣（取消点赞）: userId={}, videoId={}", userId, videoId);
    }

    /**
     * 处理收藏事件
     */
    private void handleVideoFavorite(Long userId, Long videoId) {
        String interestKey = RedisKeyConstants.Recommend.getUserInterestKey(userId);
        redisTemplate.opsForZSet().incrementScore(interestKey, "video:" + videoId, 5.0);
        log.info("更新用户兴趣（收藏）: userId={}, videoId={}", userId, videoId);
    }

    /**
     * 处理分享事件
     */
    private void handleVideoShare(Long userId, Long videoId) {
        String interestKey = RedisKeyConstants.Recommend.getUserInterestKey(userId);
        redisTemplate.opsForZSet().incrementScore(interestKey, "video:" + videoId, 2.0);
        log.info("更新用户兴趣（分享）: userId={}, videoId={}", userId, videoId);
    }

    /**
     * 死信队列处理
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_RECOMMEND_UPDATE_DLQ)
    public void handleDeadLetter(BaseEvent event) {
        log.error("推荐更新死信消息: eventId={}, eventType={}", event.getEventId(), event.getEventType());
        // TODO: 发送告警通知
        // Spring AMQP 自动 ACK
    }
}
