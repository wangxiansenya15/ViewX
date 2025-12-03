package com.flowbrain.viewx.service;

import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.pojo.vo.UserActionMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RecommendConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_RECOMMEND_UPDATE)
    public void updateProfile(UserActionMessage msg) {
        System.out.println("🧠 更新推荐头像：" + msg);
        // TODO: 更新 Redis 用户头像 / pgvector 用户偏好
    }
}

