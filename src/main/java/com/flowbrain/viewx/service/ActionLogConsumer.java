package com.flowbrain.viewx.service;

import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.dao.ActionLogMapper;
import com.flowbrain.viewx.pojo.entity.ActionLog;
import com.flowbrain.viewx.pojo.vo.UserActionMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActionLogConsumer {

    @Autowired
    private ActionLogMapper actionLogMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ACTION_LOG)
    public void saveLog(UserActionMessage msg) {
        System.out.println("📄 保存用户行为日志：" + msg);
        
        ActionLog log = new ActionLog();
        log.setId(com.flowbrain.viewx.util.IdGenerator.nextId());
        log.setUserId(msg.getUserId());
        log.setActionType(msg.getActionType());
        log.setVideoId(msg.getVideoId());
        // IP and Device info would typically come from the message if captured at controller level
        // For now we leave them null or assume message has them (need to update message VO if so)
        
        try {
            actionLogMapper.insert(log);
        } catch (Exception e) {
            System.err.println("Failed to save action log: " + e.getMessage());
        }
    }
}
