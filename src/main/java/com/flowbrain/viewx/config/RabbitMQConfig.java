package com.flowbrain.viewx.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置类
 * 支持：死信队列、延迟队列、消息持久化、自动重试
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // ==================== 交换机定义 ====================
    public static final String EXCHANGE_NAME = "viewx.exchange";
    public static final String DLX_EXCHANGE = "viewx.dlx.exchange";  // 死信交换机
    public static final String DELAY_EXCHANGE = "viewx.delay.exchange";  // 延迟交换机

    // ==================== 队列定义 ====================
    // 行为日志队列
    public static final String QUEUE_ACTION_LOG = "viewx.action.log";
    public static final String QUEUE_ACTION_LOG_DLQ = "viewx.action.log.dlq";

    // 推荐更新队列
    public static final String QUEUE_RECOMMEND_UPDATE = "viewx.recommend.update";
    public static final String QUEUE_RECOMMEND_UPDATE_DLQ = "viewx.recommend.update.dlq";

    // 通知队列
    public static final String QUEUE_NOTIFICATION = "viewx.notification";
    public static final String QUEUE_NOTIFICATION_DLQ = "viewx.notification.dlq";

    // 邮件队列
    public static final String QUEUE_EMAIL = "viewx.email";
    public static final String QUEUE_EMAIL_DLQ = "viewx.email.dlq";

    // 统计队列
    public static final String QUEUE_STATISTICS = "viewx.statistics";

    // 搜索索引更新队列
    public static final String QUEUE_SEARCH_INDEX = "viewx.search.index";

    // 视频处理队列（转码、截图等）
    public static final String QUEUE_VIDEO_PROCESS = "viewx.video.process";

    // 延迟队列（用于定时任务）
    public static final String QUEUE_DELAY = "viewx.delay";

    // ==================== 路由键定义 ====================
    public static final String ROUTING_KEY_LOG = "log";
    public static final String ROUTING_KEY_RECOMMEND = "recommend";
    public static final String ROUTING_KEY_NOTIFICATION = "notification";
    public static final String ROUTING_KEY_EMAIL = "email";
    public static final String ROUTING_KEY_STATISTICS = "statistics";
    public static final String ROUTING_KEY_SEARCH = "search";
    public static final String ROUTING_KEY_VIDEO_PROCESS = "video.process";
    public static final String ROUTING_KEY_DELAY = "delay";

    // ==================== 基础配置 ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        
        // 开启消息确认机制
        template.setMandatory(true);
        
        // 消息发送失败回调
        template.setReturnsCallback(returned -> {
            System.err.println("消息发送失败: " + returned.getMessage());
            System.err.println("回复码: " + returned.getReplyCode());
            System.err.println("回复文本: " + returned.getReplyText());
            System.err.println("交换机: " + returned.getExchange());
            System.err.println("路由键: " + returned.getRoutingKey());
        });
        
        return template;
    }

    // ==================== 交换机配置 ====================

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    // ==================== 队列配置（带死信队列） ====================

    /**
     * 行为日志队列
     */
    @Bean
    public Queue actionLogQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_LOG + ".dlq");
        args.put("x-message-ttl", 300000);  // 5分钟 TTL
        return new Queue(QUEUE_ACTION_LOG, true, false, false, args);
    }

    @Bean
    public Queue actionLogDLQ() {
        return new Queue(QUEUE_ACTION_LOG_DLQ, true);
    }

    /**
     * 推荐更新队列
     */
    @Bean
    public Queue recommendQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_RECOMMEND + ".dlq");
        return new Queue(QUEUE_RECOMMEND_UPDATE, true, false, false, args);
    }

    @Bean
    public Queue recommendDLQ() {
        return new Queue(QUEUE_RECOMMEND_UPDATE_DLQ, true);
    }

    /**
     * 通知队列
     */
    @Bean
    public Queue notificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_NOTIFICATION + ".dlq");
        return new Queue(QUEUE_NOTIFICATION, true, false, false, args);
    }

    @Bean
    public Queue notificationDLQ() {
        return new Queue(QUEUE_NOTIFICATION_DLQ, true);
    }

    /**
     * 邮件队列
     */
    @Bean
    public Queue emailQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_EMAIL + ".dlq");
        return new Queue(QUEUE_EMAIL, true, false, false, args);
    }

    @Bean
    public Queue emailDLQ() {
        return new Queue(QUEUE_EMAIL_DLQ, true);
    }

    /**
     * 统计队列
     */
    @Bean
    public Queue statisticsQueue() {
        return new Queue(QUEUE_STATISTICS, true);
    }

    /**
     * 搜索索引队列
     */
    @Bean
    public Queue searchIndexQueue() {
        return new Queue(QUEUE_SEARCH_INDEX, true);
    }

    /**
     * 视频处理队列
     */
    @Bean
    public Queue videoProcessQueue() {
        return new Queue(QUEUE_VIDEO_PROCESS, true);
    }

    /**
     * 延迟队列
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_DELAY);
        return new Queue(QUEUE_DELAY, true, false, false, args);
    }

    // ==================== 绑定配置 ====================

    @Bean
    public Binding bindingActionLog() {
        return BindingBuilder.bind(actionLogQueue()).to(mainExchange()).with(ROUTING_KEY_LOG);
    }

    @Bean
    public Binding bindingActionLogDLQ() {
        return BindingBuilder.bind(actionLogDLQ()).to(dlxExchange()).with(ROUTING_KEY_LOG + ".dlq");
    }

    @Bean
    public Binding bindingRecommend() {
        return BindingBuilder.bind(recommendQueue()).to(mainExchange()).with(ROUTING_KEY_RECOMMEND);
    }

    @Bean
    public Binding bindingRecommendDLQ() {
        return BindingBuilder.bind(recommendDLQ()).to(dlxExchange()).with(ROUTING_KEY_RECOMMEND + ".dlq");
    }

    @Bean
    public Binding bindingNotification() {
        return BindingBuilder.bind(notificationQueue()).to(mainExchange()).with(ROUTING_KEY_NOTIFICATION);
    }

    @Bean
    public Binding bindingNotificationDLQ() {
        return BindingBuilder.bind(notificationDLQ()).to(dlxExchange()).with(ROUTING_KEY_NOTIFICATION + ".dlq");
    }

    @Bean
    public Binding bindingEmail() {
        return BindingBuilder.bind(emailQueue()).to(mainExchange()).with(ROUTING_KEY_EMAIL);
    }

    @Bean
    public Binding bindingEmailDLQ() {
        return BindingBuilder.bind(emailDLQ()).to(dlxExchange()).with(ROUTING_KEY_EMAIL + ".dlq");
    }

    @Bean
    public Binding bindingStatistics() {
        return BindingBuilder.bind(statisticsQueue()).to(mainExchange()).with(ROUTING_KEY_STATISTICS);
    }

    @Bean
    public Binding bindingSearchIndex() {
        return BindingBuilder.bind(searchIndexQueue()).to(mainExchange()).with(ROUTING_KEY_SEARCH);
    }

    @Bean
    public Binding bindingVideoProcess() {
        return BindingBuilder.bind(videoProcessQueue()).to(mainExchange()).with(ROUTING_KEY_VIDEO_PROCESS);
    }

    @Bean
    public Binding bindingDelay() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(ROUTING_KEY_DELAY);
    }
}
