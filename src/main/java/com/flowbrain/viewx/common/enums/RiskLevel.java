package com.flowbrain.viewx.common.enums;

/**
 * 风险等级枚举
 */
public enum RiskLevel {
    LOW("低风险", "直接放行"),
    MEDIUM("中风险", "轻度验证"),
    HIGH("高风险", "严格验证");

    private final String label;
    private final String description;

    RiskLevel(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
