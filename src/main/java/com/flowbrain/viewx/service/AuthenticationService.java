package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.RedisKeyConstants;
import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.common.enums.Role;
import com.flowbrain.viewx.pojo.dto.UserDTO;
import com.flowbrain.viewx.common.enums.UserStatus;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private SecurityAuditService securityAuditService;

    public Result<UserDTO> authenticate(UserDTO userDTO, jakarta.servlet.http.HttpServletRequest request) {
        // 1. 获取客户端信息
        String ipAddress = com.flowbrain.viewx.util.HttpRequestUtils.getClientIpAddress(request);
        String userAgent = com.flowbrain.viewx.util.HttpRequestUtils.getUserAgent(request);

        log.info("登录请求 - 用户名/邮箱: {}, IP: {}, UA: {}",
                userDTO.getUsername() != null ? userDTO.getUsername() : userDTO.getEmail(),
                ipAddress, userAgent);

        // 检测登录方式：如果提供了验证码，则使用邮箱验证码登录
        boolean isEmailCodeLogin = (userDTO.getVerificationCode() != null &&
                !userDTO.getVerificationCode().trim().isEmpty());

        if (isEmailCodeLogin) {
            // 邮箱验证码登录流程
            return authenticateWithEmailCode(userDTO, ipAddress, userAgent, request);
        }

        // 2. 风险评估（用户名密码登录）
        com.flowbrain.viewx.common.enums.RiskLevel riskLevel = riskAssessmentService.assessLoginRisk(
                userDTO.getUsername(), ipAddress, userAgent);

        log.info("风险评估完成 - 用户名: {}, 风险等级: {}", userDTO.getUsername(), riskLevel.getLabel());

        // 3. 根据风险等级决定是否需要人机验证
        if (riskLevel == com.flowbrain.viewx.common.enums.RiskLevel.MEDIUM ||
                riskLevel == com.flowbrain.viewx.common.enums.RiskLevel.HIGH) {

            // 中高风险需要验证 CAPTCHA
            boolean captchaVerified = captchaService.verifyCaptcha(userDTO.getCaptchaToken(), ipAddress);
            if (!captchaVerified) {
                log.warn("人机验证失败 - 用户名: {}, IP: {}, 风险等级: {}",
                        userDTO.getUsername(), ipAddress, riskLevel.getLabel());

                // 注意：这里不记录登录失败次数，因为这不是密码错误，只是需要完成人机验证
                // 只记录审计日志即可
                securityAuditService.recordCaptchaFailure(userDTO.getUsername(), ipAddress,
                        "turnstile", riskLevel);
                securityAuditService.recordLoginAttempt(null, userDTO.getUsername(), false,
                        "需要人机验证", ipAddress, userAgent, riskLevel, true, false);

                // 返回明确的验证码错误，让前端知道需要显示验证码
                return Result.badRequest("请完成人机验证后重试");
            }
            log.info("人机验证通过 - 用户名: {}, 风险等级: {}", userDTO.getUsername(), riskLevel.getLabel());
        } else {
            log.info("低风险请求，跳过人机验证 - 用户名: {}", userDTO.getUsername());
        }

        // 4. 执行身份认证
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
                return Result.badRequest("用户不存在");
            }

            if (user.getDetails() == null) {
                return Result.badRequest("用户详细信息缺失");
            }

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            // 获取用户状态信息，用于前端检查用户状态是否正常
            UserStatus userStatus = user.getStatus();

            log.info("用户状态检查 - 用户名: {}, 状态: {}",
                    userDetails.getUsername(), userStatus.getLabel());

            UserDTO responseUserDTO = new UserDTO(user.getId(), token, userDetails.getUsername(), roles, userStatus);

            // 5. 登录成功后的风险记录更新
            riskAssessmentService.clearLoginFailure(userDTO.getUsername(), ipAddress);
            riskAssessmentService.recordKnownDevice(userDTO.getUsername(), ipAddress);

            // 解锁账户（如果之前被锁定）
            userService.unlockAccount(user.getId());

            // 记录成功登录到数据库
            boolean captchaWasRequired = (riskLevel != com.flowbrain.viewx.common.enums.RiskLevel.LOW);
            securityAuditService.recordLoginAttempt(user.getId(), userDTO.getUsername(), true,
                    null, ipAddress, userAgent, riskLevel, captchaWasRequired, captchaWasRequired);

            log.info("登录成功，返回200 OK响应");
            return Result.success("登录成功", responseUserDTO);

        } catch (org.springframework.security.authentication.DisabledException e) {
            log.warn("登录失败: 账户已被禁用，用户名: {}", userDTO.getUsername());
            riskAssessmentService.recordLoginFailure(userDTO.getUsername(), ipAddress);
            securityAuditService.recordLoginAttempt(null, userDTO.getUsername(), false,
                    "账户已被禁用", ipAddress, userAgent, riskLevel, false, null);
            return Result.forbidden("账户已被禁用，请联系管理员");
        } catch (org.springframework.security.authentication.LockedException e) {
            log.warn("登录失败: 账户已被锁定，用户名: {}", userDTO.getUsername());
            // 账户已锁定，不再记录失败次数
            return Result.forbidden("账户已被锁定，请联系管理员");
        } catch (org.springframework.security.authentication.AccountExpiredException e) {
            log.warn("登录失败: 账户已过期，用户名: {}", userDTO.getUsername());
            riskAssessmentService.recordLoginFailure(userDTO.getUsername(), ipAddress);
            return Result.forbidden("账户已过期，请联系管理员");
        } catch (org.springframework.security.authentication.CredentialsExpiredException e) {
            log.warn("登录失败: 凭证已过期，用户名: {}", userDTO.getUsername());
            riskAssessmentService.recordLoginFailure(userDTO.getUsername(), ipAddress);
            return Result.forbidden("凭证已过期，请重置密码");
        } catch (BadCredentialsException e) {
            log.warn("登录失败: 用户名或密码错误，用户名: {}", userDTO.getUsername());

            // 记录失败并检查是否需要锁定账户
            boolean shouldLock = riskAssessmentService.recordLoginFailure(userDTO.getUsername(), ipAddress);

            // 获取当前失败次数
            int failureCount = riskAssessmentService.getLoginFailureCount(userDTO.getUsername(), ipAddress);
            int remainingAttempts = 10 - failureCount; // 10次机会

            securityAuditService.recordLoginAttempt(null, userDTO.getUsername(), false,
                    "用户名或密码错误", ipAddress, userAgent, riskLevel, false, null);

            // 如果失败次数达到10次，锁定账户
            if (shouldLock) {
                // 锁定用户账户
                try {
                    User user = userService.getUserByUsername(userDTO.getUsername());
                    if (user != null) {
                        userService.lockAccount(user.getId());
                        log.warn("账户已被锁定 - 用户名: {}, 原因: 登录失败次数过多", userDTO.getUsername());
                    }
                } catch (Exception lockEx) {
                    log.error("锁定账户失败", lockEx);
                }
                return Result.forbidden("登录失败次数过多，账户已被锁定。请重置密码或联系管理员");
            }

            // 返回错误消息，包含剩余次数
            String message;
            if (remainingAttempts > 5) {
                // 剩余次数大于5次，只显示剩余次数
                message = String.format("用户名或密码错误，还有 %d 次尝试机会", remainingAttempts);
            } else if (remainingAttempts > 0) {
                // 剩余次数5次或更少，提醒可以重置密码
                message = String.format("用户名或密码错误，还有 %d 次尝试机会。如果忘记密码，可以点击\"忘记密码\"重置，否则账户会被锁定", remainingAttempts);
            } else {
                message = "用户名或密码错误";
            }
            return Result.badRequest(message);
        } catch (Exception e) {
            log.error("登录失败", e);
            riskAssessmentService.recordLoginFailure(userDTO.getUsername(), ipAddress);
            return Result.serverError("登录过程中发生异常，请稍后再试");
        }
    }

    /**
     * 验证验证码
     * 从Redis中检查用户输入的验证码是否与存储的验证码匹配
     * 
     * @param email            用户邮箱
     * @param verificationCode 用户输入的验证码
     * @return 验证结果
     */
    public Result<?> verifyCode(String email, String verificationCode) {
        try {
            // 参数校验
            if (email == null || email.trim().isEmpty()) {
                return Result.badRequest("邮箱不能为空");
            }
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return Result.badRequest("验证码不能为空");
            }

            // 从Redis中获取存储的验证码
            String redisKey = RedisKeyConstants.Captcha.getVerificationCodeKey(email);
            String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

            if (storedCode == null) {
                log.warn("验证码验证失败: Redis中未找到验证码，邮箱: {}", email);
                return Result.badRequest("验证码已过期或不存在，请重新获取");
            }

            // 验证码比较（忽略大小写和前后空格）
            if (!storedCode.trim().equalsIgnoreCase(verificationCode.trim())) {
                log.warn("验证码验证失败: 验证码不匹配，邮箱: {}", email);
                return Result.badRequest("验证码错误");
            }

            // 验证成功后立即删除验证码，防止重复使用
            stringRedisTemplate.delete(redisKey);
            log.info("验证码验证成功，已删除Redis中的验证码，邮箱: {}", email);

            return Result.success("验证码验证成功", null);

        } catch (Exception ex) {
            log.error("验证码验证过程中发生异常，邮箱: {}", email, ex);
            return Result.serverError("验证码验证失败，请稍后再试");
        }
    }

    /**
     * 重置密码
     * 通过验证码验证后重置用户密码
     * 
     * @param email            用户邮箱
     * @param verificationCode 验证码
     * @param newPassword      新密码
     * @return 重置结果
     */
    public Result<?> resetPassword(String email, String verificationCode, String newPassword) {
        try {
            // 参数校验
            if (email == null || email.trim().isEmpty()) {
                return Result.badRequest("邮箱不能为空");
            }
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return Result.badRequest("验证码不能为空");
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return Result.badRequest("新密码不能为空");
            }
            if (newPassword.length() < 6 || newPassword.length() > 20) {
                return Result.badRequest("密码长度应为6-20个字符");
            }

            // 首先验证验证码（验证成功后会自动从Redis删除）
            Result<?> verifyResult = verifyCode(email, verificationCode);
            if (verifyResult.getCode() != Result.OK) {
                return verifyResult; // 验证码验证失败，直接返回错误结果
            }

            // 根据邮箱查找用户
            User user = userService.getUserByEmail(email);
            if (user == null) {
                log.warn("重置密码失败: 用户不存在，邮箱: {}", email);
                return Result.notFound("用户不存在");
            }

            // 加密新密码
            String encodedPassword = passwordEncoder.encode(newPassword);

            // 更新用户密码
            boolean updateResult = userService.updateUserPassword(user.getId(), encodedPassword);
            if (!updateResult) {
                log.error("重置密码失败: 数据库更新失败，邮箱: {}", email);
                return Result.serverError("密码重置失败，请稍后再试");
            }

            log.info("密码重置成功，邮箱: {}", email);
            return Result.success("密码重置成功");

        } catch (Exception ex) {
            log.error("重置密码过程中发生异常，邮箱: {}", email, ex);
            return Result.serverError("密码重置失败，请稍后再试");
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

    /**
     * 邮箱验证码登录
     * 
     * @param userDTO   包含邮箱和验证码的用户信息
     * @param ipAddress 客户端IP
     * @param userAgent 用户代理
     * @param request   HTTP请求
     * @return 登录结果
     */
    private Result<UserDTO> authenticateWithEmailCode(UserDTO userDTO, String ipAddress,
            String userAgent, jakarta.servlet.http.HttpServletRequest request) {
        log.info("邮箱验证码登录 - 邮箱: {}", userDTO.getEmail());

        // 1. 参数校验
        if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            return Result.badRequest("邮箱不能为空");
        }
        if (userDTO.getVerificationCode() == null || userDTO.getVerificationCode().trim().isEmpty()) {
            return Result.badRequest("验证码不能为空");
        }

        // 2. 验证验证码
        Result<?> verifyResult = verifyCode(userDTO.getEmail(), userDTO.getVerificationCode());
        if (verifyResult.getCode() != Result.OK) {
            log.warn("邮箱验证码登录失败: 验证码错误，邮箱: {}", userDTO.getEmail());
            return Result.badRequest("验证码错误或已过期");
        }

        // 3. 根据邮箱查找用户
        User user = userService.getUserByEmail(userDTO.getEmail());
        if (user == null) {
            log.warn("邮箱验证码登录失败: 用户不存在，邮箱: {}", userDTO.getEmail());
            return Result.notFound("该邮箱未注册");
        }

        // 4. 检查账户状态
        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("邮箱验证码登录失败: 账户已被禁用，邮箱: {}", userDTO.getEmail());
            return Result.forbidden("账户已被禁用，请联系管理员");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            log.warn("邮箱验证码登录失败: 账户已被锁定，邮箱: {}", userDTO.getEmail());
            return Result.forbidden("账户已被锁定，请联系管理员");
        }

        // 5. 生成Token
        String token = jwtUtils.generateAndStoreToken(user.getUsername());
        log.info("邮箱验证码登录成功，生成JWT令牌，邮箱: {}, 用户名: {}", userDTO.getEmail(), user.getUsername());

        // 6. 构建返回的用户信息
        List<String> roles = List.of("ROLE_USER"); // 默认角色，可以从数据库获取
        UserDTO responseUserDTO = new UserDTO(user.getId(), token, user.getUsername(), roles, user.getStatus());

        // 7. 记录登录成功
        riskAssessmentService.clearLoginFailure(user.getUsername(), ipAddress);
        riskAssessmentService.recordKnownDevice(user.getUsername(), ipAddress);
        userService.unlockAccount(user.getId());

        // 记录到审计日志
        com.flowbrain.viewx.common.enums.RiskLevel riskLevel = com.flowbrain.viewx.common.enums.RiskLevel.LOW;
        securityAuditService.recordLoginAttempt(user.getId(), user.getUsername(), true,
                "邮箱验证码登录", ipAddress, userAgent, riskLevel, false, false);

        log.info("邮箱验证码登录成功，返回200 OK响应");
        return Result.success("登录成功", responseUserDTO);
    }
}
