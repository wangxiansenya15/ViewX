package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.common.Role;
import com.flowbrain.viewx.dao.SocialUserMapper;
import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.pojo.dto.UserDTO;
import com.flowbrain.viewx.pojo.entity.SocialUser;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.util.IdGenerator;
import com.flowbrain.viewx.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OAuth2Service {

    @Autowired
    private SocialUserMapper socialUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Handle OAuth2 Login Success
     * 1. Check if social user exists
     * 2. If yes, login directly
     * 3. If no, create new user (auto-register) or bind to existing (logic needed)
     * 4. Return JWT
     */
    @Transactional
    public Result<UserDTO> handleLogin(String provider, String providerUserId, String email, String nickname, String avatarUrl) {
        log.info("OAuth2 login: {} - {}", provider, providerUserId);

        SocialUser socialUser = socialUserMapper.selectByProvider(provider, providerUserId);
        User user;

        if (socialUser == null) {
            // New User: Auto Register
            log.info("New social user, auto registering...");
            
            // 1. Create Main User
            user = new User();
            user.setId(IdGenerator.nextId());
            // Generate random username if conflict or use prefix
            String username = provider + "_" + UUID.randomUUID().toString().substring(0, 8);
            user.setUsername(username);
            user.setEmail(email != null ? email : username + "@placeholder.com"); // Handle missing email
            user.setPassword("OAUTH2_NO_PASSWORD"); // Placeholder
            user.setRole(Role.USER);
            
            userMapper.insertUser(user);
            userMapper.insertUserDetail(user.getId());

            // 2. Create Social Link
            socialUser = new SocialUser();
            socialUser.setId(IdGenerator.nextId());
            socialUser.setUserId(user.getId());
            socialUser.setProvider(provider);
            socialUser.setProviderUserId(providerUserId);
            socialUser.setNickname(nickname);
            socialUser.setAvatarUrl(avatarUrl);
            socialUser.setEmail(email);
            
            socialUserMapper.insert(socialUser);
        } else {
            // Existing User
            user = userMapper.selectUserById(socialUser.getUserId());
            if (user == null) {
                return Result.error(500, "Linked user not found");
            }
        }

        // Generate Token
        String token = jwtUtils.generateAndStoreToken(user.getUsername());
        
        // Return DTO
        UserDTO userDTO = new UserDTO(user.getId(), token, user.getUsername(), List.of(user.getRole().name()), user.getStatus());
        return Result.success("Login successful", userDTO);
    }
}
