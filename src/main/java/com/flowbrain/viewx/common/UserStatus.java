package com.flowbrain.viewx.common;

import lombok.Getter;

@Getter
public enum UserStatus {
    /**
     * 用户状态
     */
    ACTIVE("正常", "green", true, true, true, true),
    DISABLED("账户禁用", "red", false, true, true, true),
    EXPIRED("已过期", "orange", true, false, true, true),
    LOCKED("账户已锁定", "orange", true, true, false, true),
    CREDENTIALS_EXPIRED("用户凭证过期", "orange", true, true, true, false);

    private final String label;
    private final String color;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;

    //构造函数
    UserStatus(String label, String color, boolean enabled,
               boolean accountNonExpired, boolean accountNonLocked,
               boolean credentialsNonExpired) {
        this.label = label;
        this.color = color;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
    }

    // 根据四个布尔值解析状态
    public static UserStatus fromBooleans(boolean enabled, boolean accountNonExpired,
                                          boolean accountNonLocked, boolean credentialsNonExpired) {
        if (!enabled) return DISABLED;
        if (!accountNonExpired) return EXPIRED;
        if (!accountNonLocked) return LOCKED;
        if (!credentialsNonExpired) return CREDENTIALS_EXPIRED;
        return ACTIVE;
    }

}
