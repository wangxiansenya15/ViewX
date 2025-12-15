package com.flowbrain.viewx.pojo.dto;

import com.flowbrain.viewx.common.enums.NotificationType;
import lombok.Data;

/**
 * 通知查询参数
 */
@Data
public class NotificationQueryDTO {

    /**
     * 通知类型 (可选)
     */
    private NotificationType notificationType;

    /**
     * 是否只查询未读 (可选)
     */
    private Boolean unreadOnly;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 20;
}
