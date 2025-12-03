package com.flowbrain.viewx.service;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private JavaMailSender javaMailSender;

    /**
     * 发送验证码到指定邮箱并存储在会话中
     *
     * @param email 接收验证码的邮箱地址，不能为空
     * @param session HTTP会话对象，用于存储生成的验证码
     * @return JSON格式的响应结果，包含发送状态和消息。成功时返回验证码发送成功信息，失败时返回错误信息
     */
    public String sendVerificationCode(String email, HttpSession session) {
        if (email == null || email.isEmpty()) {
            return "邮箱不能为空";
        }
        // 生成6位随机验证码
        String code = RandomUtil.randomNumbers(6); // 生成纯数字验证码

        // 存储验证码到会话中
        session.setAttribute("verificationCode", code);

        // 发送邮件
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom("2994512097@qq.com");
//        message.setTo(email);
//        message.setSubject("FlowBrain——ViewX平台，给你发的安全验证码");
//        //message.setText("您的验证码是：" + code + "，5分钟内有效，为确保您的数据安全🔐请勿向他人泄漏。");
//        message.setText("尊敬的FlowBrain用户：\n\n" +
//                "您的验证码是：%s\n\n" +
//                "此验证码5分钟内有效，请及时完成验证。\n\n" +
//                "如果不是您本人操作，请忽略此邮件。\n\n" +
//                "FlowBrain团队" + code);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("2994512097@qq.com");
        message.setTo(email);
        message.setSubject("【FlowBrain】安全验证码");

        String emailContent =
                "FlowBrain - ViewX 平台\n" +
                        "========================\n\n" +
                        "亲爱的用户：\n\n" +
                        "您正在进行的操作需要安全验证。\n\n" +
                        "验证码：" + code + "\n\n" +
                        "有效期：5分钟\n\n" +
                        "安全提示：\n" +
                        "• 请勿向任何人泄露此验证码\n" +
                        "• 如果不是您本人操作，请忽略此邮件\n" +
                        "• 如有疑问，请联系客服\n\n" +
                        "感谢您使用 FlowBrain 服务！\n\n" +
                        "FlowBrain 团队";

        message.setText(emailContent);

        try {
            javaMailSender.send(message);

            // 使用 Jackson 将 Map 转换为 JSON 字符串
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(Map.of("success", true, "message", "验证码已发送，请注意查收"));
        }
        //  处理发送邮件异常
        catch (Exception e) {
            log.error("邮件发送失败", e);
            return "{\"success\": false, \"message\": \"邮件发送失败，请检查日志\"}";
        }
    }

}