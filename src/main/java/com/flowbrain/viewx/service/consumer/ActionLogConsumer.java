package com.flowbrain.viewx.service.consumer;

import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.dao.ActionLogMapper;
import com.flowbrain.viewx.pojo.dto.BaseEvent;
import com.flowbrain.viewx.pojo.entity.ActionLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 行为日志消费者
 * 功能：记录所有用户行为到数据库，用于数据分析和审计
 */
@Service
@Slf4j
public class ActionLogConsumer {

    @Autowired
    private ActionLogMapper actionLogMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ACTION_LOG)
    public void handleActionLog(BaseEvent event) {
        try {
            log.info("接收到行为日志事件: eventId={}, eventType={}, userId={}",
                    event.getEventId(), event.getEventType(), event.getUserId());

            // 创建行为日志记录
            ActionLog actionLog = new ActionLog();
            actionLog.setUserId(event.getUserId());
            actionLog.setActionType(event.getEventType());

            // 从事件数据中提取 videoId
            if (event.getData() != null && event.getData().containsKey("videoId")) {
                Object videoIdObj = event.getData().get("videoId");
                if (videoIdObj instanceof Long) {
                    actionLog.setVideoId((Long) videoIdObj);
                } else if (videoIdObj instanceof Integer) {
                    actionLog.setVideoId(((Integer) videoIdObj).longValue());
                } else if (videoIdObj instanceof String) {
                    actionLog.setVideoId(Long.parseLong((String) videoIdObj));
                }
            }

            // 保存到数据库（ID 和 createdAt 会自动生成）
            actionLogMapper.insert(actionLog);

            log.info("行为日志保存成功: eventId={}", event.getEventId());

            // Spring AMQP 会自动 ACK 成功处理的消息

        } catch (Exception e) {
            log.error("处理行为日志失败: eventId={}", event.getEventId(), e);

            // 抛出异常，让 Spring AMQP 根据配置决定是否重试
            // 如果配置了重试机制，消息会重新入队
            // 如果超过重试次数，会发送到死信队列
            throw new RuntimeException("处理行为日志失败", e);
        }
    }

    /**
     * 死信队列消费者
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ACTION_LOG_DLQ)
    public void handleDeadLetter(BaseEvent event) {
        log.error("收到死信消息: eventId={}, eventType={}, userId={}",
                event.getEventId(), event.getEventType(), event.getUserId());

        // 记录到错误日志表或发送告警
        // TODO: 实现告警机制

        // 死信消息通常不需要重新处理，直接确认即可
        // Spring AMQP 会自动 ACK
    }
}
