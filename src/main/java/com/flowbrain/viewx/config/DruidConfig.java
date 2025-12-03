package com.flowbrain.viewx.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource.druid")
@Getter
@Setter
public class DruidConfig {
    private int initialSize;
    private int minIdle;
    private int maxActive;

    private int connectTimeout;
    private int socketTimeout;
    private int transactionQueryTimeout;
    private int queryTimeout;
    private int removeAbandonedTimeout;
    private String filters;

    // 嵌套配置
    private Wall wall = new Wall();

    @Getter
    @Setter
    public static class Wall {
        private boolean enabled;
        private Config config = new Config();

        @Getter
        @Setter
        public static class Config {
            private boolean selectAllow;
            private int selectLimit;
            private boolean deleteWhereNone;
            private boolean updateWhereNone;
            private boolean metadataPerm;
        }
    }
}
