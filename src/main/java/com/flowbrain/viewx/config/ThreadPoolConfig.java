package com.flowbrain.viewx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {
    /**
     * 核心业务线程池
     */
    @Bean("coreThreadPool")
    public Executor coreThreadPool(ThreadPoolProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 从配置文件读取参数
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        
        // 拒绝策略：由调用者线程执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅停机
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    @Bean(name = "TaskExecutor")
    public Executor productTaskExecutor(ThreadPoolProperties properties) {
        // 复用配置或使用另一套配置
        return coreThreadPool(properties); 
    }
}
