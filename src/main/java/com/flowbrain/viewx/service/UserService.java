package com.flowbrain.viewx.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flowbrain.viewx.dao.UserMapper;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.common.enums.UserStatus;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.util.IdGenerator;
import com.flowbrain.viewx.util.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /**
     * 插入用户
     * 使用事务确保数据一致性，如果操作失败会自动回滚
     * 
     * @param user 用户对象
     *             必填：用户名、密码
     *             可选：其他字段（如角色、状态等）
     * @return Result<User> 表示插入结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> insertUser(User user) {
        log.info("开始插入用户，用户名: {}, 邮箱: {}", user.getUsername(), user.getEmail());

        // 参数验证
        if (user.getUsername() == null || user.getUsername().isEmpty() || user.getPassword() == null
                || user.getPassword().isEmpty()) {
            return Result.badRequest("用户名或密码不能为空");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 验证用户名是否已存在
        if (existsByUsername(user.getUsername())) {
            log.warn("用户名已存在: {}", user.getUsername());
            return Result.badRequest("用户名已存在");
        }

        // 验证邮箱是否已被注册
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            User existingUser = getUserByEmail(user.getEmail());
            if (existingUser != null) {
                log.warn("邮箱已被注册: {}", user.getEmail());
                return Result.badRequest("邮箱已注册");
            }
        }

        try {
            // 插入用户记录
            user.setId(IdGenerator.nextId());
            int insertedUser = userMapper.insertUser(user);

            int insertedDetail = userMapper.insertUserDetail(user.getId());

            if ((insertedUser & insertedDetail) > 0) {
                log.info("用户创建成功，用户名: {}", user.getUsername());
                return Result.success("用户创建成功", "账号为：" + user.getUsername());
            }

            log.error("用户创建失败，用户名: {}", user.getUsername());
            return Result.serverError("用户创建失败,请稍后重试");
        } catch (Exception e) {
            log.error("用户创建过程中发生异常，用户名: {}", user.getUsername(), e);
            throw e; // 让事务管理器处理回滚
        }
    }

    /**
     * 根据ID删除用户
     * 使用事务确保数据一致性，如果操作失败会自动回滚
     * 
     * @param id 用户ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteUserById(Long id) {
        if (id == null) {
            return Result.badRequest("ID不能为空");
        }
        return userMapper.deleteUserById(id)
                ? Result.success("删除成功")
                : Result.serverError("删除失败");
    }

    /**
     * 更新用户信息
     * 使用事务确保数据一致性，如果操作失败会自动回滚
     * 使用MyBatis-Plus的updateById方法，自动处理动态SQL，只更新非null字段
     * 
     * @param user 用户信息
     * @return Result<Void>
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateUser(User user) {
        if (user.getId() == null) {
            return Result.badRequest("用户ID不能为空");
        }

        User existingUser = userMapper.selectById(user.getId());
        if (existingUser == null) {
            return Result.notFound("用户不存在");
        }

        // 使用MyBatis-Plus的updateById，只更新非null字段
        int updated = userMapper.updateById(user);
        return updated > 0 ? Result.success("更新成功") : Result.serverError("更新失败");
    }

    /**
     * 根据ID获取用户名
     *
     * @param id 用户ID
     * @return 用户名，如果不存在返回 null
     */
    public String getUsernameById(Long id) {
        if (id == null)
            return null;
        User user = userMapper.selectById(id);
        return user != null ? user.getUsername() : null;
    }

    /**
     * 根据ID获取用户
     *
     * @param id 用户ID
     * @return 用户对象
     */
    public Result<User> getUserById(Long id) {
        if (id == null) {
            return Result.badRequest("ID不能为空");
        }

        try {
            User user = userMapper.selectUserById(id);
            return user != null
                    ? Result.success(user)
                    : Result.notFound("用户不存在");
        } catch (Exception e) {
            log.error("获取用户失败", e);
            return Result.serverError("获取用户失败");
        }
    }

    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    public List<User> getAllUsers() {
        return userMapper.selectAllUsers();
    }

    public Result<Page<User>> getUserList(int page, int size) {
        try {
            Page<User> pageObj = PageUtils.createPage(page, size);
            Page<User> result = userMapper.selectPage(pageObj, null);
            log.info("获取用户列表成功，总数: {}", result.getTotal());
            return Result.success(result);
        } catch (RuntimeException e) {
            log.error("获取用户列表失败", e);
            return Result.serverError("获取用户列表失败");
        }
    }

    public boolean existsByUsername(String username) {
        return userMapper.selectCount(new QueryWrapper<User>().eq("username", username)) > 0;
    }

    public User getUserByUsername(String username) {
        log.info("开始根据用户名 {} 查询用户", username);
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            log.warn("未找到用户名为 {} 的用户", username);
            return null;
        }

        // 加载用户详情（使用新的方法，正确映射字段并关联 UserDetail）
        try {
            user = userMapper.selectUserWithDetailsById(user.getId());
            if (user != null) {
                log.info("成功找到用户: {}，详情加载状态: {}",
                        user.getUsername(),
                        user.getDetails() != null ? "已加载" : "未找到");
            }
        } catch (Exception e) {
            log.error("加载用户详情失败，用户名: {}", username, e);
            // 即使加载详情失败，也返回基本用户信息
        }

        return user;
    }

    /**
     * 根据邮箱获取用户
     * 用于密码重置功能中根据邮箱查找用户
     * 
     * @param email 用户邮箱
     * @return 用户对象，如果不存在则返回null
     */
    public User getUserByEmail(String email) {
        log.info("开始根据邮箱 {} 查询用户", email);
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", email));
        if (user == null) {
            log.info("未找到邮箱为 {} 的用户", email);
        } else {
            // 延迟加载用户详情
            user = userMapper.selectUserById(user.getId());
            log.info("成功找到用户: {}, 邮箱: {}", user.getUsername(), email);
        }
        return user;
    }

    /**
     * 更新用户密码
     * 用于密码重置功能，直接更新用户的加密密码
     * 
     * @param userId          用户ID
     * @param encodedPassword 已加密的新密码
     * @return 是否更新成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserPassword(Long userId, String encodedPassword) {
        log.info("开始更新用户密码，用户ID: {}", userId);
        try {
            int updated = userMapper.updateUserPassword(userId, encodedPassword);
            if (updated > 0) {
                log.info("用户密码更新成功，用户ID: {}", userId);
                return true;
            } else {
                log.warn("用户密码更新失败，用户ID: {}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("更新用户密码时发生异常，用户ID: {}, 异常信息: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    public Result<UserStatus> updateStatus(Long id, UserStatus status) {
        log.info("更新用户状态，用户ID: {}, 状态: {}", id, status);

        // 检查用户是否存在
        User user = userMapper.selectUserById(id);
        if (user == null) {
            log.warn("用户不存在，ID: {}", id);
            return Result.notFound("用户不存在");
        }

        // 记录更新前的状态值，用于调试
        log.info("更新前的状态值 - enabled: {}, accountNonExpired: {}, accountNonLocked: {}, credentialsNonExpired: {}",
                user.isEnabled(), user.isAccountNonExpired(), user.isAccountNonLocked(),
                user.isCredentialsNonExpired());

        // 记录要更新的状态值
        log.info("要更新的状态值 - enabled: {}, accountNonExpired: {}, accountNonLocked: {}, credentialsNonExpired: {}",
                status.isEnabled(), status.isAccountNonExpired(), status.isAccountNonLocked(),
                status.isCredentialsNonExpired());

        try {
            // 执行更新操作
            log.info("执行SQL更新操作，用户ID: {}", id);
            int updated = userMapper.updateStatus(
                    id,
                    status.isEnabled(),
                    status.isAccountNonExpired(),
                    status.isAccountNonLocked(),
                    status.isCredentialsNonExpired());

            log.info("SQL执行结果: updated={}", updated);

            if (updated > 0) {
                // 再次获取用户，验证更新是否生效
                User updatedUser = userMapper.selectUserById(id);
                log.info(
                        "更新后的状态值 - enabled: {}, accountNonExpired: {}, accountNonLocked: {}, credentialsNonExpired: {}",
                        updatedUser.isEnabled(), updatedUser.isAccountNonExpired(),
                        updatedUser.isAccountNonLocked(), updatedUser.isCredentialsNonExpired());

                log.info("用户状态更新成功，用户ID: {}", id);
                return Result.success("用户状态更新成功", status);
            } else {
                log.warn("用户状态更新失败，SQL执行未影响任何行，用户ID: {}", id);
                return Result.badRequest("用户状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新用户状态时发生异常，用户ID: {}, 异常信息: {}", id, e.getMessage(), e);
            return Result.serverError("更新用户状态时发生异常: " + e.getMessage());
        }
    }

    /**
     * 处理登录失败，当失败次数达到阈值时锁定账户
     * 
     * @param username     用户名
     * @param attemptCount 当前失败次数
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> handleLoginFailure(String username, Integer attemptCount) {
        log.info("处理登录失败，用户名: {}, 失败次数: {}", username, attemptCount);

        // 检查失败次数是否达到锁定阈值
        if (attemptCount >= 5) {
            // 获取用户信息
            User user = getUserByUsername(username);
            if (user == null) {
                log.warn("用户不存在，无法锁定账户，用户名: {}", username);
                return Result.notFound("用户不存在");
            }

            try {
                // 锁定账户：将 account_non_locked 设置为 false
                int updated = userMapper.updateStatus(
                        user.getId(),
                        user.isEnabled(),
                        user.isAccountNonExpired(),
                        false, // 锁定账户
                        user.isCredentialsNonExpired());

                if (updated > 0) {
                    log.warn("账户已被锁定，用户名: {}, 失败次数: {}", username, attemptCount);
                    return Result.success("账户因多次登录失败已被锁定");
                } else {
                    log.error("锁定账户失败，用户名: {}", username);
                    return Result.serverError("锁定账户失败");
                }
            } catch (Exception e) {
                log.error("锁定账户时发生异常，用户名: {}, 异常信息: {}", username, e.getMessage(), e);
                return Result.serverError("锁定账户时发生异常: " + e.getMessage());
            }
        } else {
            log.info("登录失败次数未达到锁定阈值，用户名: {}, 当前失败次数: {}", username, attemptCount);
            return Result.success("登录失败已记录，失败次数: " + attemptCount);
        }
    }

    /**
     * 搜索用户（通过用户名或昵称）
     * 
     * @param keyword 搜索关键词
     * @param page    页码
     * @param size    每页大小
     * @return 用户列表
     */
    public Result<List<com.flowbrain.viewx.pojo.vo.UserSummaryVO>> searchUsers(String keyword, int page, int size) {
        try {
            int offset = (page - 1) * size;
            List<com.flowbrain.viewx.pojo.vo.UserSummaryVO> users = userMapper.searchUsers(keyword, offset, size);

            log.info("搜索用户成功，关键词: {}, 结果数: {}", keyword, users.size());
            return Result.success(users);
        } catch (Exception e) {
            log.error("搜索用户失败，关键词: {}", keyword, e);
            return Result.serverError("搜索用户失败");
        }
    }

    /**
     * 锁定账户
     * 
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockAccount(Long userId) {
        log.warn("锁定账户 - 用户ID: {}", userId);
        User user = userMapper.selectById(userId);
        if (user != null) {
            userMapper.updateStatus(
                    userId,
                    user.isEnabled(),
                    user.isAccountNonExpired(),
                    false, // 锁定账户
                    user.isCredentialsNonExpired());
        }
    }

    /**
     * 解锁账户（登录成功后调用）
     * 
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlockAccount(Long userId) {
        log.info("解锁账户 - 用户ID: {}", userId);
        User user = userMapper.selectById(userId);
        if (user != null && !user.isAccountNonLocked()) {
            userMapper.updateStatus(
                    userId,
                    user.isEnabled(),
                    user.isAccountNonExpired(),
                    true, // 解锁账户
                    user.isCredentialsNonExpired());
        }
    }
}
