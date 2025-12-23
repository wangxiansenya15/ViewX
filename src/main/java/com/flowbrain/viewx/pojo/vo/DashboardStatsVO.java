package com.flowbrain.viewx.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsVO {
    private long totalUsers;
    private double userTrend; // Percentage increase compared to older data

    private long totalVideos;
    private double videoTrend;

    private long totalViews; // Or "todayViews" if we can track it
    private double viewTrend;

    private long storageUsed; // In bytes
    private double storageTrend;
}
