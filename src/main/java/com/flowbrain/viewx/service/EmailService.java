package com.flowbrain.viewx.service;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowbrain.viewx.common.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    // 验证码有效期（秒）
    private static final long VERIFICATION_CODE_EXPIRE = 300; // 5分钟

    // 发件人邮箱
    private static final String FROM_EMAIL = "2994512097@qq.com";

    /**
     * 邮件类型枚举
     */
    public enum EmailType {
        REGISTER("注册账号"),
        RESET_PASSWORD("重置密码"),
        LOGIN_VERIFY("登录验证");

        private final String description;

        EmailType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 生成验证码并存储到Redis，然后异步发送邮件
     * 此方法会立即返回，不等待邮件发送完成
     *
     * @param email 接收验证码的邮箱地址，不能为空
     * @return JSON格式的响应结果
     */
    public String sendVerificationCode(String email) {
        return sendVerificationCode(email, EmailType.REGISTER);
    }

    /**
     * 生成验证码并存储到Redis，然后异步发送邮件（支持指定邮件类型）
     *
     * @param email     接收验证码的邮箱地址
     * @param emailType 邮件类型
     * @return JSON格式的响应结果
     */
    public String sendVerificationCode(String email, EmailType emailType) {
        if (email == null || email.isEmpty()) {
            return "邮箱不能为空";
        }

        // 生成6位随机验证码
        String code = RandomUtil.randomNumbers(6);

        // 存储验证码到Redis中，设置5分钟过期时间
        String redisKey = RedisKeyConstants.Captcha.getVerificationCodeKey(email);
        stringRedisTemplate.opsForValue().set(redisKey, code, Duration.ofSeconds(VERIFICATION_CODE_EXPIRE));
        log.info("验证码已存储到Redis，邮箱: {}, 类型: {}, Key: {}, 有效期: {}秒",
                email, emailType.getDescription(), redisKey, VERIFICATION_CODE_EXPIRE);

        // 异步发送邮件（不阻塞响应）
        sendEmailAsync(email, code, emailType);

        // 立即返回成功响应
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(Map.of("success", true, "message", "验证码已发送，请注意查收"));
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return "{\"success\": true, \"message\": \"验证码已发送，请注意查收\"}";
        }
    }

    /**
     * 异步发送验证码邮件
     * 使用@Async注解，此方法会在独立线程中执行，不阻塞主线程
     *
     * @param email     接收邮件的地址
     * @param code      验证码
     * @param emailType 邮件类型
     */
    @Async
    public void sendEmailAsync(String email, String code, EmailType emailType) {
        log.info("开始异步发送验证码邮件到: {}, 类型: {}", email, emailType.getDescription());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_EMAIL);
        message.setTo(email);
        message.setSubject(buildEmailSubject(emailType));
        message.setText(buildEmailContent(code, emailType));

        try {
            javaMailSender.send(message);
            log.info("验证码邮件发送成功，邮箱: {}, 类型: {}", email, emailType.getDescription());
        } catch (Exception e) {
            log.error("验证码邮件发送失败，邮箱: {}, 类型: {}", email, emailType.getDescription(), e);
            // 注意：异步方法中的异常不会传播到调用者
            // 可以考虑添加重试机制或者通知机制
        }
    }

    /**
     * 构建邮件主题
     *
     * @param emailType 邮件类型
     * @return 邮件主题
     */
    private String buildEmailSubject(EmailType emailType) {
        return switch (emailType) {
            case REGISTER -> "【FlowBrain】注册验证码";
            case RESET_PASSWORD -> "【FlowBrain】密码重置验证码";
            case LOGIN_VERIFY -> "【FlowBrain】登录验证码";
        };
    }

    /**
     * 构建邮件内容
     *
     * @param code      验证码
     * @param emailType 邮件类型
     * @return 邮件内容
     */
    private String buildEmailContent(String code, EmailType emailType) {
        String purpose = switch (emailType) {
            case REGISTER -> "注册 FlowBrain 账号";
            case RESET_PASSWORD -> "重置您的账号密码";
            case LOGIN_VERIFY -> "登录验证";
        };

        return "FlowBrain - ViewX 平台\n" +
                "========================\n\n" +
                "亲爱的用户：\n\n" +
                "您正在进行" + purpose + "操作。\n\n" +
                "验证码：" + code + "\n\n" +
                "有效期：5分钟\n\n" +
                "安全提示：\n" +
                "• 请勿向任何人泄露此验证码\n" +
                "• 如果不是您本人操作，请忽略此邮件\n" +
                "• 如有疑问，请联系客服\n\n" +
                "感谢您使用 FlowBrain 服务！\n\n" +
                "FlowBrain 团队";
    }

}