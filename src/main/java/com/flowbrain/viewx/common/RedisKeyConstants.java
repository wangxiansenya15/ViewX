package com.flowbrain.viewx.common;

/**
 * Redis Key 常量类
 * 统一管理所有Redis Key前缀和模板
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {
        // 防止实例化
    }

    // 基础前缀
    public static final String KEY_PREFIX = "com.flowbrain:viewx:";

    // Token相关
    public static final String TOKEN_KEY = "tokens:";
    public static final String BLACKLIST_KEY = "blacklist:";

    // 用户相关
    public static final String USER_KEY = "users:";
    public static final String USER_ONLINE_KEY = "online:users:";
    public static final String USER_SESSION_KEY = "sessions:";

    // 验证码相关
    public static final String CAPTCHA_KEY = "captcha:";
    public static final String VERIFICATION_CODE_KEY = "verification:code:";

    // 权限相关
    public static final String PERMISSION_KEY = "permissions:";
    public static final String ROLE_KEY = "roles:";

    // 业务相关
    public static final String PRODUCT_KEY = "video:";
    public static final String ORDER_KEY = "ai:";
    public static final String CART_KEY = "recommendation:";

    // 系统相关
    public static final String CONFIG_KEY = "config:";
    public static final String LOCK_KEY = "lock:";
    public static final String RATE_LIMIT_KEY = "rate:limit:";

    /**
     * 构建完整的Redis Key
     */
    public static String buildKey(String... parts) {
        StringBuilder keyBuilder = new StringBuilder(KEY_PREFIX);
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                keyBuilder.append(part);
            }
        }
        return keyBuilder.toString();
    }

    /**
     * Token相关的Key构建方法
     */
    public static class Token {
        public static String getUserTokenKey(String username) {
            return buildKey(TOKEN_KEY, username);
        }

        public static String getBlacklistKey(String token) {
            return buildKey(BLACKLIST_KEY, token);
        }

        public static String getOnlineUserKey(String username) {
            return buildKey(USER_ONLINE_KEY, username);
        }
    }

    /**
     * 用户相关的Key构建方法
     */
    public static class User {
        public static String getUserInfoKey(Long userId) {
            return buildKey(USER_KEY, "info:", String.valueOf(userId));
        }

        public static String getUserInfoKey(String username) {
            return buildKey(USER_KEY, "info:", username);
        }

        public static String getUserPermissionsKey(Integer userId) {
            return buildKey(USER_KEY, "permissions:", String.valueOf(userId));
        }

        public static String getUserSessionKey(String sessionId) {
            return buildKey(USER_SESSION_KEY, sessionId);
        }
    }

    /**
     * 验证码相关的Key构建方法
     */
    public static class Captcha {
        public static String getCaptchaKey(String identifier) {
            return buildKey(CAPTCHA_KEY, identifier);
        }

        public static String getVerificationCodeKey(String email) {
            return buildKey(VERIFICATION_CODE_KEY, email);
        }
    }

    /**
     * 业务相关的Key构建方法
     */
    public static class Business {
        public static String getProductKey(Integer productId) {
            return buildKey(PRODUCT_KEY, String.valueOf(productId));
        }

        public static String getOrderKey(Integer orderId) {
            return buildKey(ORDER_KEY, String.valueOf(orderId));
        }

        public static String getUserCartKey(Integer userId) {
            return buildKey(CART_KEY, String.valueOf(userId));
        }
    }

    /**
     * 系统相关的Key构建方法
     */
    public static class System {
        public static String getConfigKey(String configName) {
            return buildKey(CONFIG_KEY, configName);
        }

        public static String getLockKey(String lockName) {
            return buildKey(LOCK_KEY, lockName);
        }

        public static String getRateLimitKey(String resource, String identifier) {
            return buildKey(RATE_LIMIT_KEY, resource, ":", identifier);
        }
    }
}