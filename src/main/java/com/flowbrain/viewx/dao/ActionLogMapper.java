package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.ActionLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActionLogMapper {

    @Insert("INSERT INTO vx_action_logs (id, user_id, action_type, video_id, ip_address, device_info, created_at) " +
            "VALUES (#{id}, #{userId}, #{actionType}, #{videoId}, #{ipAddress}, #{deviceInfo}, NOW())")
    void insert(ActionLog log);
}
