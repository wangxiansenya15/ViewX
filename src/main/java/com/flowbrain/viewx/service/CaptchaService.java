package com.flowbrain.viewx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 人机验证服务
 * 支持 Cloudflare Turnstile 和自定义滑块验证
 */
@Slf4j
@Service
public class CaptchaService {

    @Value("${captcha.type:slider}")
    private String captchaType;

    @Value("${captcha.site-key:}")
    private String siteKey;

    @Value("${captcha.secret-key:}")
    private String secretKey;

    @Value("${captcha.enabled:true}")
    private boolean captchaEnabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Cloudflare Turnstile 验证 URL
    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    public CaptchaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 验证人机验证 token
     *
     * @param token    前端返回的验证 token
     * @param remoteIp 用户 IP 地址
     * @return 验证是否成功
     */
    public boolean verifyCaptcha(String token, String remoteIp) {
        // 如果未启用验证,直接返回 true
        if (!captchaEnabled) {
            log.debug("人机验证未启用,跳过验证");
            return true;
        }

        if (token == null || token.isEmpty()) {
            log.warn("验证 token 为空");
            return false;
        }

        try {
            switch (captchaType.toLowerCase()) {
                case "turnstile":
                    return verifyTurnstile(token, remoteIp);
                case "slider":
                    // 简单的滑块验证,前端已完成,后端只需检查 token 格式
                    return "slider-verified".equals(token);
                default:
                    log.error("不支持的验证类型: {}", captchaType);
                    return false;
            }
        } catch (Exception e) {
            log.error("验证人机验证时发生错误", e);
            return false;
        }
    }

    /**
     * 验证 Cloudflare Turnstile
     */
    private boolean verifyTurnstile(String token, String remoteIp) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", secretKey);
            params.add("response", token);
            if (remoteIp != null && !remoteIp.isEmpty()) {
                params.add("remoteip", remoteIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(TURNSTILE_VERIFY_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                boolean success = jsonNode.get("success").asBoolean();

                if (success) {
                    log.info("Turnstile 验证成功");
                } else {
                    log.warn("Turnstile 验证失败: {}", response.getBody());
                }

                return success;
            }
        } catch (Exception e) {
            log.error("验证 Turnstile 时发生错误", e);
        }
        return false;
    }

    /**
     * 检查是否需要人机验证
     * 可以根据不同的场景返回不同的结果
     *
     * @param action 操作类型 (login, register, comment, etc.)
     * @param userId 用户 ID (可选)
     * @return 是否需要验证
     */
    public boolean requiresCaptcha(String action, Long userId) {
        if (!captchaEnabled) {
            return false;
        }

        // 根据不同的操作类型决定是否需要验证
        switch (action.toLowerCase()) {
            case "login":
            case "register":
            case "reset-password":
                // 登录、注册、重置密码始终需要验证
                return true;

            case "comment":
            case "post":
            case "upload":
                // 发布内容需要验证
                return true;

            case "like":
            case "vote":
                // 点赞、投票需要验证
                return true;

            default:
                // 其他操作默认不需要验证
                return false;
        }
    }

    /**
     * 获取验证配置信息(返回给前端)
     */
    public Map<String, Object> getCaptchaConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", captchaEnabled);
        config.put("type", captchaType);
        config.put("siteKey", siteKey);
        // 注意:不要返回 secret key,只返回 site key
        return config;
    }
}
