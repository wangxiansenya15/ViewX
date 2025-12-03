package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.vo.UserActionMessage;
import com.flowbrain.viewx.service.ActionProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

@RestController
@RequestMapping("/email")
@CrossOrigin(origins = "*")
public class EmailController {

    private final JavaMailSender mailSender;

    public EmailController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送违纪通知邮件
     * POST /api/email/violation-notice
     * Content-Type: application/json
     *
     * 请求体示例：
     * {
     *   "email": "198xxx@163.com",
     *   "studentName": "xxx",
     *   "violationType": "旷课",
     *   "punishmentDetails": "累计旷课超过10节，给予警告处分"
     * }
     */
    @PostMapping("/msg")
    public ResponseEntity<Map<String, Object>> sendViolationNotice(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证必要字段
            String email = request.get("email");
            String studentName = request.get("studentName");
            String violationType = request.get("violationType");
            String punishmentDetails = request.get("punishmentDetails");

            if (email == null || studentName == null || violationType == null || punishmentDetails == null) {
                response.put("success", false);
                response.put("message", "缺少必要参数：email, studentName, violationType, punishmentDetails");
                return ResponseEntity.badRequest().body(response);
            }

            // 发送文本邮件
            boolean textSuccess = sendTextViolationEmail(email, studentName, violationType, punishmentDetails);

            // 发送HTML邮件（备用）
            boolean htmlSuccess = true;
            if (!textSuccess) {
                htmlSuccess = sendHtmlViolationEmail(email, studentName, violationType, punishmentDetails);
            }

            if (textSuccess || htmlSuccess) {
                response.put("success", true);
                response.put("message", "违纪通知邮件发送成功");
                response.put("email", email);
                response.put("type", textSuccess ? "text" : "html");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "邮件发送失败，请检查邮箱配置");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "系统错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 发送文本格式违纪通知
     */
    private boolean sendTextViolationEmail(String email, String studentName,
                                           String violationType, String punishmentDetails) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("2994512097@qq.com");
            message.setTo(email);
            message.setSubject("【文理学院】违纪处分通知 - " + studentName);

            String emailContent =
                    "违纪处分通知\n\n" +
                            "学生姓名：" + studentName + "\n" +
                            "违纪类型：" + violationType + "\n" +
                            "处分详情：\n" + punishmentDetails + "\n\n" +
                            "注意事项：\n" +
                            "• 请认真对待此次处分通知\n" +
                            "• 如有异议，请在3个工作日内联系教务处\n" +
                            "• 遵守校规校纪，共同维护良好的学习环境\n\n" +
                            "文理学院学生管理系统\n" +
                            "发送时间：" + new Date();

            message.setText(emailContent);
            mailSender.send(message);

            System.out.println("文本违纪通知邮件发送成功至: " + email);
            return true;

        } catch (Exception e) {
            System.err.println("文本邮件发送失败: " + e.getMessage());
            return false;
        }
    }

    @Autowired
    private  ActionProducer producer;



    @PostMapping("/send")
    public Result<String> send(@RequestBody UserActionMessage msg) {
        producer.sendAction(msg);
        return Result.success("消息已发送：" + msg.getActionType());
    }

    /**
     * 发送HTML格式违纪通知
     */
    private boolean sendHtmlViolationEmail(String email, String studentName,
                                           String violationType, String punishmentDetails) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("2994512097@qq.com");
            helper.setTo(email);
            helper.setSubject("【四川文理学院】违纪处分通知 - " + studentName);

            String htmlContent = createViolationHtmlContent(studentName, violationType, punishmentDetails);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("HTML违纪通知邮件发送成功至: " + email);
            return true;

        } catch (Exception e) {
            System.err.println("HTML邮件发送失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建HTML邮件内容
     */
    private String createViolationHtmlContent(String studentName, String violationType, String punishmentDetails) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                "body { font-family: 'Microsoft YaHei', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px 20px; text-align: center; }" +
                ".content { padding: 30px 20px; background: #f8f9fa; }" +
                ".violation-card { background: white; padding: 25px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); margin: 20px 0; }" +
                ".info-item { margin: 15px 0; padding: 10px; border-left: 4px solid #667eea; background: #f8f9fa; }" +
                ".important { color: #dc3545; font-weight: bold; background: #ffe6e6; padding: 10px; border-radius: 5px; }" +
                ".footer { text-align: center; padding: 20px; color: #6c757d; font-size: 14px; border-top: 1px solid #dee2e6; margin-top: 20px; }" +
                ".badge { background: #ffc107; color: #856404; padding: 5px 10px; border-radius: 15px; font-size: 14px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"header\">" +
                "<h1>FlowBrain 学生管理系统</h1>" +
                "<h2>违纪处分通知</h2>" +
                "</div>" +
                "<div class=\"content\">" +
                "<div class=\"violation-card\">" +
                "<p>尊敬的 <strong>" + studentName + "</strong> 同学：</p>" +
                "<div class=\"info-item\">" +
                "<p><strong>违纪类型：</strong><span class=\"badge\">" + violationType + "</span></p>" +
                "<p><strong>处分详情：</strong></p>" +
                "<p>" + punishmentDetails.replace("\n", "<br>") + "</p>" +
                "</div>" +
                "<div class=\"important\">" +
                "<p>📌 重要提示：</p>" +
                "<ul>" +
                "<li>请认真对待此次处分通知</li>" +
                "<li>如有异议，请在3个工作日内联系教务处</li>" +
                "<li>遵守校规校纪，共同维护良好的学习环境</li>" +
                "<li>此通知将记入学生档案，请务必重视</li>" +
                "</ul>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<p>FlowBrain 学生管理系统 · 教务处</p>" +
                "<p>发送时间：" + new Date() + "</p>" +
                "<p>如有疑问，请联系：教务处办公室</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Email Service");
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }
}
