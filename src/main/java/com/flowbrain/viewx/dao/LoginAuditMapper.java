package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.LoginAudit;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录审计 Mapper
 */
@Mapper
public interface LoginAuditMapper {

    /**
     * 插入登录审计记录
     */
    @Insert("INSERT INTO vx_login_audit (user_id, username, success, failure_reason, " +
            "ip_address, user_agent, risk_level, " +
            "captcha_required, captcha_verified, login_time) " +
            "VALUES (#{userId}, #{username}, #{success}, #{failureReason}, " +
            "#{ipAddress}, #{userAgent}, #{riskLevel}, " +
            "#{captchaRequired}, #{captchaVerified}, #{loginTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LoginAudit loginAudit);

    /**
     * 根据用户ID查询登录历史
     */
    @Select("SELECT * FROM vx_login_audit WHERE user_id = #{userId} ORDER BY login_time DESC LIMIT #{limit}")
    List<LoginAudit> findByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 根据IP地址查询登录历史
     */
    @Select("SELECT * FROM vx_login_audit WHERE ip_address = #{ipAddress} ORDER BY login_time DESC LIMIT #{limit}")
    List<LoginAudit> findByIpAddress(@Param("ipAddress") String ipAddress, @Param("limit") int limit);

    /**
     * 查询指定时间范围内的失败登录次数
     */
    @Select("SELECT COUNT(*) FROM vx_login_audit WHERE username = #{username} " +
            "AND ip_address = #{ipAddress} AND success = FALSE " +
            "AND login_time >= #{startTime}")
    int countFailuresByUsernameAndIp(@Param("username") String username,
            @Param("ipAddress") String ipAddress,
            @Param("startTime") LocalDateTime startTime);

    /**
     * 查询高风险登录记录
     */
    @Select("SELECT * FROM vx_login_audit WHERE risk_level = 'HIGH' " +
            "AND login_time >= #{startTime} ORDER BY login_time DESC")
    List<LoginAudit> findHighRiskLogins(@Param("startTime") LocalDateTime startTime);
}
