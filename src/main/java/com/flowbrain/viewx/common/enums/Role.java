package com.flowbrain.viewx.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum Role {
    SUPER_ADMIN("超级管理员"),
    ADMIN("管理员"),
    USER("普通用户"),
    REVIEWER("审核员");

    @EnumValue // This tells MyBatis-Plus to use the enum name (SUPER_ADMIN, ADMIN, etc.) for
               // database storage
    private final String name;
    private final String label;

    Role(String label) {
        this.name = this.name();
        this.label = label;
    }

}
