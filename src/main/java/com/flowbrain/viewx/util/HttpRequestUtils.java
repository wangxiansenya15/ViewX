package com.flowbrain.viewx.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 请求工具类
 */
@Slf4j
public class HttpRequestUtils {

    /**
     * 获取客户端真实IP地址
     * 考虑了代理、负载均衡等情况
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 1. 优先检查 Cloudflare 的 header (如果您使用了 Cloudflare CDN)
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 2. 检查 X-Forwarded-For (标准代理头)
        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index).trim();
            } else {
                return ip;
            }
        }

        // 3. 检查其他常见代理头
        String[] headers = {
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }

        // 4. 最后获取直接连接的 IP
        ip = request.getRemoteAddr();

        // 5. 特殊处理本地 IPv6 回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * 获取User-Agent
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
}
