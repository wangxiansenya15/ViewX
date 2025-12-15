package com.flowbrain.viewx.config;

import com.flowbrain.viewx.common.Role;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.service.TokenService;
import com.flowbrain.viewx.service.UserService;
import com.flowbrain.viewx.util.JwtAuthenticationFilter;
import com.flowbrain.viewx.util.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * @author: wxs
 * @date: 2025/05/17 15:15
 * @description: Spring Security 配置类
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtUtils, userDetailsService);
    }

    /**
     * 安全过滤器链配置（6.x核心配置）
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Lazy JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // 公开访问路径
                        .requestMatchers("/auth/**",
                                "/products/**",
                                "/oauth2/**",
                                "/email/**",
                                "/store/**",
                                "/system/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/recommend/**")
                        .permitAll()
                        // 允许匿名访问视频详情和交互状态
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/videos/**", "/interactions/**")
                        .permitAll()
                        // 角色权限控制
                        .requestMatchers("/comments/**", "/user/**", "/avatar/**", "/payment/**", "/msg/**",
                                "/videos/**", "/interactions/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "USER")
                        .requestMatchers("/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        // 其他请求需要认证
                        .anyRequest().authenticated())
                // 异常处理
                .exceptionHandling(e -> e
                        .accessDeniedPage("/403"))
                // 禁用CSRF（适用于REST API场景）
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 设置会话管理策略为无状态（适用于JWT认证）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 允许 OAuth2 使用 Session
                )
                // 开启 OAuth2 登录
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/oauth2/success", true) // 登录成功后跳转到自定义 Controller
                );

        return http.build();
    }

    /**
     * 数据库实现的UserDetailsService
     */
    @Bean
    public UserDetailsService userDetailsService(UserService userService) {
        return username -> {
            // 通过UserService从数据库加载用户
            User user = userService.getUserByUsername(username);
            // 如果用户不存在，抛出异常
            if (user == null) {
                throw new UsernameNotFoundException("用户不存在");
            }

            // 检查 UserDetail 是否为 null
            Role role = user.getRole();
            if (role == null) {
                throw new BadCredentialsException("用户详细信息不存在");
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPassword())
                    .roles(new String[] { (user.getRole().name()) })
                    .build();
        };
    }

    /**
     * BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*")); // 使用 Pattern 支持端口通配符
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // 允许的请求方法
        config.setAllowedHeaders(List.of("*")); // 允许所有请求头
        config.setExposedHeaders(List.of("Authorization")); // 暴露给前端的头
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 预检请求缓存时间

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        return builder.build();
    }
}