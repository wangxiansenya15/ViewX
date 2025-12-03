package com.flowbrain.viewx.common;

import lombok.Getter;

@Getter
public enum Role {
    SUPER_ADMIN("超级管理员"),
    ADMIN("管理员"),
    USER("普通用户"),
    REVIEWER("审核员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

}
