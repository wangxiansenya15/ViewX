package com.flowbrain.viewx.pojo.dto;

import lombok.Data;

/**
 * 消息传输对象（前端发送给后端）
 */
@Data
public class MessageDTO {
    private Long receiverId;
    private String content;
    private String messageType; // TEXT, IMAGE, VIDEO, EMOJI
}
