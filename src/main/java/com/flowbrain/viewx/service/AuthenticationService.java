package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.common.Role;
import com.flowbrain.viewx.pojo.dto.UserDTO;
import com.flowbrain.viewx.common.UserStatus;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.util.JwtUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AuthenticationService {

    @Autowired
    UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    // 在类顶部添加如下注入代码：
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Result<UserDTO> authenticate(UserDTO userDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDTO.getUsername(), userDTO.getPassword()));
            log.info("用户凭证验证成功");

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtils.generateAndStoreToken(userDetails.getUsername());
            log.info("JWT令牌生成成功，token为: {}", token);

            // 获取用户详情（包含角色、头像等）
            User user = userService.getUserByUsername(userDetails.getUsername());
            if (user == null) {
                log.warn("用户不存在: {}", userDetails.getUsername());
                return Result.error(Result.BAD_REQUEST, "用户不存在");
            }

            Role role = user.getRole();
            if (role == null) {
                log.warn("用户详细信息缺失，用户名: {}", userDetails.getUsername());
                return Result.error(Result.BAD_REQUEST, "用户详细信息缺失");
            }

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            // 获取用户状态信息，用于前端检查用户状态是否正常
            UserStatus userStatus = user.getStatus();

            log.info("用户状态检查 - 用户名: {}, 状态: {}",
                    userDetails.getUsername(), userStatus.getLabel());

            UserDTO responseUserDTO = new UserDTO(user.getId(), token, userDetails.getUsername(), roles, userStatus);

            log.info("登录成功，返回200 OK响应");
            return Result.success("登录成功", responseUserDTO);

        } catch (BadCredentialsException ex) {
            log.warn("登录失败: 凭证无效 - {}", ex.getMessage());
            return Result.error(Result.BAD_REQUEST, "凭证无效,用户名或密码错误");
        } catch (Exception ex) {
            log.error("登录过程中发生异常: {}", ex.getClass().getName(), ex);
            return Result.error(Result.SERVER_ERROR, "登录过程中发生异常，请稍后再试");
        }
    }

    /**
     * 验证验证码
     * 检查用户输入的验证码是否与会话中存储的验证码匹配
     * 
     * @param email            用户邮箱
     * @param verificationCode 用户输入的验证码
     * @param session          HTTP会话对象
     * @return 验证结果
     */
    public Result<?> verifyCode(String email, String verificationCode, HttpSession session) {
        try {
            // 参数校验
            if (email == null || email.trim().isEmpty()) {
                return Result.error(Result.BAD_REQUEST, "邮箱不能为空");
            }
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return Result.error(Result.BAD_REQUEST, "验证码不能为空");
            }

            // 从会话中获取存储的验证码
            String storedCode = (String) session.getAttribute("verificationCode");
            if (storedCode == null) {
                log.warn("验证码验证失败: 会话中未找到验证码，邮箱: {}", email);
                return Result.error(Result.BAD_REQUEST, "验证码已过期或不存在，请重新获取");
            }

            // 验证码比较（忽略大小写和前后空格）
            if (!storedCode.trim().equalsIgnoreCase(verificationCode.trim())) {
                log.warn("验证码验证失败: 验证码不匹配，邮箱: {}", email);
                return Result.error(Result.BAD_REQUEST, "验证码错误");
            }

            log.info("验证码验证成功，邮箱: {}", email);
            return Result.success("验证码验证成功", null);

        } catch (Exception ex) {
            log.error("验证码验证过程中发生异常，邮箱: {}", email, ex);
            return Result.error(Result.SERVER_ERROR, "验证码验证失败，请稍后再试");
        }
    }

    /**
     * 重置密码
     * 通过验证码验证后重置用户密码
     * 
     * @param email            用户邮箱
     * @param verificationCode 验证码
     * @param newPassword      新密码
     * @param session          HTTP会话对象
     * @return 重置结果
     */
    public Result<?> resetPassword(String email, String verificationCode, String newPassword, HttpSession session) {
        try {
            // 参数校验
            if (email == null || email.trim().isEmpty()) {
                return Result.error(Result.BAD_REQUEST, "邮箱不能为空");
            }
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return Result.error(Result.BAD_REQUEST, "验证码不能为空");
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return Result.error(Result.BAD_REQUEST, "新密码不能为空");
            }
            if (newPassword.length() < 6 || newPassword.length() > 20) {
                return Result.error(Result.BAD_REQUEST, "密码长度应为6-20个字符");
            }

            // 首先验证验证码
            Result<?> verifyResult = verifyCode(email, verificationCode, session);
            if (verifyResult.getCode() != Result.OK) {
                return verifyResult; // 验证码验证失败，直接返回错误结果
            }

            // 根据邮箱查找用户
            User user = userService.getUserByEmail(email);
            if (user == null) {
                log.warn("重置密码失败: 用户不存在，邮箱: {}", email);
                return Result.error(Result.NOT_FOUND, "用户不存在");
            }

            // 加密新密码
            String encodedPassword = passwordEncoder.encode(newPassword);

            // 更新用户密码
            boolean updateResult = userService.updateUserPassword(user.getId(), encodedPassword);
            if (!updateResult) {
                log.error("重置密码失败: 数据库更新失败，邮箱: {}", email);
                return Result.error(Result.SERVER_ERROR, "密码重置失败，请稍后再试");
            }

            // 清除会话中的验证码，防止重复使用
            session.removeAttribute("verificationCode");

            log.info("密码重置成功，邮箱: {}", email);
            return Result.success("密码重置成功");

        } catch (Exception ex) {
            log.error("重置密码过程中发生异常，邮箱: {}", email, ex);
            return Result.error(Result.SERVER_ERROR, "密码重置失败，请稍后再试");
        }
    }

    @Autowired
    private TokenService tokenService;

    /**
     * 退出登录
     * 
     * @param token JWT Token
     */
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            jwtUtils.invalidateToken(token);
        }
    }

    /**
     * 验证Token有效性
     * 检查JWT签名、过期时间以及Redis中的存储状态
     * 
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 1. 验证JWT签名和格式
            if (!jwtUtils.validateToken(token)) {
                log.warn("Token验证失败: JWT签名或格式无效");
                return false;
            }

            // 2. 从Token中提取用户名
            String username = jwtUtils.getUsernameFromToken(token);
            if (username == null || username.isEmpty()) {
                log.warn("Token验证失败: 无法提取用户名");
                return false;
            }

            // 3. 检查Redis中是否存在该用户的Token（是否过期或被删除）
            if (!tokenService.validateToken(username, token)) {
                log.warn("Token验证失败: Redis中Token不存在或已过期，用户名: {}", username);
                return false;
            }

            // 4. 检查Token是否在黑名单中
            if (tokenService.isTokenBlacklisted(token)) {
                log.warn("Token验证失败: Token已被加入黑名单，用户名: {}", username);
                return false;
            }

            log.debug("Token验证成功，用户名: {}", username);
            return true;

        } catch (Exception e) {
            log.error("Token验证过程中发生异常", e);
            return false;
        }
    }
}
