package com.flowbrain.viewx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步配置
 * 启用 @Async 注解支持
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot 会自动配置默认的异步执行器
    // 如需自定义线程池，可以在这里添加 @Bean 方法
}
