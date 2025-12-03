package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.pojo.dto.LoginFailureDTO;
import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.UserDTO;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.service.AuthenticationService;
import com.flowbrain.viewx.service.EmailService;
import com.flowbrain.viewx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    private final UserService userService;
    private final AuthenticationService authService;
    private final EmailService emailService;

    // 推荐使用构造函数注入
    public AuthController(UserService userService, AuthenticationService authService, EmailService emailService) {
        this.userService = userService;
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/code")
    public String getVerificationCode(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        return emailService.sendVerificationCode(email);
    }

    @PostMapping("/register")
    public Result<?> registerUser(@RequestBody User user) {
        log.info("开始处理注册请求，用户名: {}, 邮箱: {}", user.getUsername(), user.getEmail());

        // 1. 验证必填字段
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return Result.error(Result.BAD_REQUEST, "邮箱不能为空");
        }

        // 2. 验证验证码（从 User 对象中获取前端传来的验证码）
        String verificationCode = user.getVerificationCode();
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            return Result.error(Result.BAD_REQUEST, "验证码不能为空");
        }

        // 3. 调用验证码验证服务（验证成功后会自动从Redis删除验证码）
        Result<?> verifyResult = authService.verifyCode(user.getEmail(), verificationCode);
        if (verifyResult.getCode() != Result.OK) {
            log.warn("注册失败: 验证码验证失败，邮箱: {}", user.getEmail());
            return verifyResult; // 返回验证失败的结果
        }

        // 4. 验证码验证成功，执行注册
        Result<String> registerResult = userService.insertUser(user);
        log.info("注册处理完成，用户名: {}, 结果: {}", user.getUsername(), registerResult.getMessage());

        return registerResult;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody UserDTO userDTO) {
        log.info("开始处理登录请求，用户名: {}", userDTO.getUsername());
        return authService.authenticate(userDTO);
    }

    /**
     * 处理登录失败
     * 当登录失败次数达到5次时，自动锁定用户账户
     * 
     * @param loginFailureDTO 登录失败信息
     * @return 处理结果
     */
    @PostMapping("/login-failure")
    public Result<?> handleLoginFailure(@RequestBody LoginFailureDTO loginFailureDTO) {
        log.info("处理登录失败请求，用户名: {}, 失败次数: {}, 错误类型: {}",
                loginFailureDTO.getUsername(), loginFailureDTO.getAttemptCount(), loginFailureDTO.getErrorType());

        // 记录详细的失败信息用于安全审计
        log.info("登录失败详情 - IP: {}, UserAgent: {}, 时间: {}",
                loginFailureDTO.getIpAddress(), loginFailureDTO.getUserAgent(), loginFailureDTO.getTimestamp());

        return userService.handleLoginFailure(loginFailureDTO.getUsername(), loginFailureDTO.getAttemptCount());
    }

    /**
     * 验证验证码
     * 用于注册时验证邮箱验证码是否正确
     * 
     * @param payload 包含邮箱和验证码的请求体
     * @return 验证结果
     */
    @PostMapping("/verify")
    public Result<?> verifyCode(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String verificationCode = payload.get("verificationCode");

        log.info("开始验证验证码，邮箱: {}", email);

        return authService.verifyCode(email, verificationCode);
    }

    /**
     * 重置密码
     * 通过验证码验证后重置用户密码
     * 
     * @param payload 包含邮箱、验证码和新密码的请求体
     * @return 重置结果
     */
    @PostMapping("/reset")
    public Result<?> resetPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String verificationCode = payload.get("verificationCode");
        String newPassword = payload.get("newPassword");

        log.info("开始重置密码，邮箱: {}", email);

        return authService.resetPassword(email, verificationCode, newPassword);
    }

    /**
     * 验证Token有效性
     * 前端可以定期调用此接口检查Token是否过期
     */
    @PostMapping("/validate")
    public Result<?> validateToken(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");

        if (token == null || token.isEmpty()) {
            return Result.error(400, "请求参数错误，Token不能为空！");
        }

        try {
            // 调用 AuthenticationService 的验证方法
            boolean isValid = authService.validateToken(token);

            if (isValid) {
                return Result.success("Token有效");
            } else {
                return Result.error(401, "Token已失效或过期，请重新登录");
            }
        } catch (Exception e) {
            log.error("Token验证失败", e);
            return Result.error(401, "Token验证失败: " + e.getMessage());
        }
    }

    /**
     * 退出登录
     * 将Token加入黑名单，使其立即失效
     */
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            authService.logout(token);
            return Result.success("退出登录成功");
        }
        return Result.success("退出登录成功"); // 即使没有Token也返回成功，方便前端处理
    }

}