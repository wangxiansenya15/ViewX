package com.flowbrain.viewx.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "file.storage")
public class ResourceStorageConfig {

    // 存储类型: local, aliyun-oss, aws-s3
    private String storageType = "local";

    // 基础配置
    private String baseUrl;
    private String uploadDir;

    // 本地存储配置
    private Local local = new Local();

    // 阿里云OSS配置
    private AliyunOss oss = new AliyunOss();


    @Data
    public static class Local {
        private String domain;
    }

    @Data
    public static class AliyunOss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
    }
}