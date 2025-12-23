package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.SecurityEvent;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全事件 Mapper
 */
@Mapper
public interface SecurityEventMapper {

    /**
     * 插入安全事件
     */
    @Insert("INSERT INTO vx_security_events (event_type, severity, username, " +
            "ip_address, description, event_time) " +
            "VALUES (#{eventType}, #{severity}, #{username}, " +
            "#{ipAddress}, #{description}, #{eventTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SecurityEvent event);

    /**
     * 查询未处理的高危事件
     */
    @Select("SELECT * FROM vx_security_events WHERE severity IN ('HIGH', 'CRITICAL') " +
            "AND handled = FALSE ORDER BY event_time DESC LIMIT #{limit}")
    List<SecurityEvent> findUnhandledCriticalEvents(@Param("limit") int limit);

    /**
     * 标记事件为已处理
     */
    @Update("UPDATE vx_security_events SET handled = TRUE WHERE id = #{id}")
    int markAsHandled(@Param("id") Long id);

    /**
     * 查询指定时间范围内的事件
     */
    @Select("SELECT * FROM vx_security_events WHERE event_time >= #{startTime} " +
            "AND event_time <= #{endTime} ORDER BY event_time DESC")
    List<SecurityEvent> findByTimeRange(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据IP地址查询事件
     */
    @Select("SELECT * FROM vx_security_events WHERE ip_address = #{ipAddress} " +
            "ORDER BY event_time DESC LIMIT #{limit}")
    List<SecurityEvent> findByIpAddress(@Param("ipAddress") String ipAddress,
            @Param("limit") int limit);
}
