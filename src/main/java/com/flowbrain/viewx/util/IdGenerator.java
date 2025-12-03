package com.flowbrain.viewx.util;


import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    /**
     * 生成雪花算法ID
     */
    public static Long nextId() {
        return IdUtil.getSnowflake().nextId();
    }

    /**
     * 生成指定workerId的ID（用于分布式环境）
     */
    public static Long nextId(long workerId, long datacenterId) {
        return IdUtil.getSnowflake(workerId, datacenterId).nextId();
    }

    /**
     * 生成UUID（不含横线）
     */
    public static String uuid() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 生成对象ID（用于缓存key等）
     */
    public static String objectId() {
        return IdUtil.objectId();
    }
}