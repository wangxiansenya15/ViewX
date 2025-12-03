package com.flowbrain.viewx.util;

import com.flowbrain.viewx.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Autowired
    private TokenService tokenService;

    // @Override
    // protected void doFilterInternal(HttpServletRequest request,
    // HttpServletResponse response, FilterChain filterChain)
    // throws ServletException, IOException {
    //
    // // 首先检查是否应该跳过过滤
    // if (shouldNotFilter(request)) {
    // log.info("跳过JWT验证，公开访问路径: {}", request.getRequestURI());
    // filterChain.doFilter(request, response);
    // return;
    // }
    //
    // String token = jwtUtils.getTokenFromRequest(request);
    // String path = request.getRequestURI();
    //
    // // 如果没有token或token无效
    // if (token == null || !jwtUtils.validateToken(token)) {
    // log.warn("无效或缺失Token，请求路径: {}", path);
    // filterChain.doFilter(request, response);
    // return;
    // }
    //
    // // Token有效，进行用户认证
    // String username = jwtUtils.getUsernameFromToken(token);
    // UserDetails userDetails;
    //
    // try {
    // userDetails = userDetailsService.loadUserByUsername(username);
    // } catch (Exception e) {
    // log.warn("无法加载用户详情: {}", e.getMessage());
    // response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未找到用户");
    // return;
    // }
    //
    // UsernamePasswordAuthenticationToken authentication = new
    // UsernamePasswordAuthenticationToken(
    // userDetails, null, userDetails.getAuthorities());
    //
    // log.info("JWT验证通过，用户：{}", username);
    // // 设置认证信息到安全上下文
    // SecurityContextHolder.getContext().setAuthentication(authentication);
    //
    // filterChain.doFilter(request, response);
    // }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("进入JWT过滤器");
        String token = jwtUtils.getTokenFromRequest(request);
        String path = request.getRequestURI();

        // 增强的Token验证
        if (token == null || !jwtUtils.validateToken(token)) {
            log.warn("无效或缺失Token，请求路径: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查Token是否在黑名单中
        if (tokenService.isTokenBlacklisted(token)) {
            log.warn("Token已被加入黑名单，请求路径: {}", path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token已失效");
            return;
        }

        // Token有效，进行用户认证
        String username = jwtUtils.getUsernameFromToken(token);

        try {
            // 验证Token是否与Redis中存储的一致（可选，取决于是否允许单点登录或仅依赖黑名单）
            // if (!tokenService.validateToken(username, token)) { ... }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 刷新Token过期时间（滑动过期）
            tokenService.refreshToken(username);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            log.info("JWT验证通过，用户：{}", username);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.warn("无法加载用户详情: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未找到用户");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 确定哪些请求应该跳过JWT过滤器
     * 
     * @param request HTTP请求
     * @return 如果请求应该跳过过滤返回true，否则返回false
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 明确定义需要放行的路径
        boolean shouldSkip = path.startsWith("/auth") ||
                path.startsWith("/static/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/login") || // 匹配 /login 和 /login/xxx
                path.equals("/favicon.ico") ||
                path.equals("/error");

        if (shouldSkip) {
            log.debug("放行路径: {}", path);
        }

        return shouldSkip;
    }
}
