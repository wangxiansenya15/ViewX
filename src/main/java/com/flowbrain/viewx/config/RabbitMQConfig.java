package com.flowbrain.viewx.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "viewx.exchange";
    public static final String QUEUE_ACTION_LOG = "user.action.log";
    public static final String QUEUE_RECOMMEND_UPDATE = "user.recommend.update";
    public static final String QUEUE_NOTIFICATION = "user.notification";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue actionLogQueue() {
        return new Queue(QUEUE_ACTION_LOG, true);
    }

    @Bean
    public Queue recommendQueue() {
        return new Queue(QUEUE_RECOMMEND_UPDATE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(QUEUE_NOTIFICATION, true);
    }

    @Bean
    public Binding bindingLog() {
        return BindingBuilder.bind(actionLogQueue()).to(exchange()).with("log");
    }

    @Bean
    public Binding bindingRecommend() {
        return BindingBuilder.bind(recommendQueue()).to(exchange()).with("recommend");
    }

    @Bean
    public Binding bindingNotification() {
        return BindingBuilder.bind(notificationQueue()).to(exchange()).with("notification");
    }
}

