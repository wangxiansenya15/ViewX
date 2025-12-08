package com.flowbrain.viewx.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置Jackson ObjectMapper，将Long类型序列化为String
     * 解决JavaScript中Long类型精度丢失问题
     * JavaScript的Number类型只能安全表示 -(2^53-1) 到 2^53-1 之间的整数
     */
    @Bean
    @Primary
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 创建自定义模块
        SimpleModule simpleModule = new SimpleModule();
        // 将Long和long类型序列化为字符串
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 使用builder构建ObjectMapper并注册模块
        return builder
                .modules(simpleModule)
                .build();
    }
}
