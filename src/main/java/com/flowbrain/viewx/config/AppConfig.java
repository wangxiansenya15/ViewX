package com.flowbrain.viewx.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

// Spring配置类

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"com.flowbrain.viewx.dao", "com.flowbrain.viewx.service", "com.flowbrain.viewx.util"})
public class AppConfig {
}