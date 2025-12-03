package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.UserDTO;
import com.flowbrain.viewx.service.OAuth2Service;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
@Slf4j
public class OAuth2Controller {

    @Autowired
    private OAuth2Service oAuth2Service;

    @Value("${viewx.frontend.url}")
    private String frontendUrl;

    /**
     * OAuth2 Login Success Callback
     * This endpoint is called by Spring Security after successful OAuth2 handshake.
     * It exchanges the OAuth2User info for our system's JWT token.
     */
    @GetMapping("/success")
    public void loginSuccess(Authentication authentication, HttpServletResponse response) throws IOException {
        // Check if authentication is null (direct access without OAuth2 flow)
        if (authentication == null) {
            log.error("No authentication found - endpoint accessed without OAuth2 flow");
            response.sendRedirect(frontendUrl + "/login?error=no_authentication");
            return;
        }
        
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            log.error("Invalid authentication type: {}", authentication.getClass().getName());
            response.sendRedirect(frontendUrl + "/login?error=invalid_auth_type");
            return;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId(); // "github" or "gitee"
        OAuth2User oauthUser = oauthToken.getPrincipal();
        Map<String, Object> attributes = oauthUser.getAttributes();

        log.info("OAuth2 Success: Provider={}, Attributes={}", registrationId, attributes);

        String providerUserId;
        String nickname;
        String avatarUrl;
        String email = null;

        // Normalize attributes based on provider
        if ("github".equalsIgnoreCase(registrationId)) {
            providerUserId = String.valueOf(attributes.get("id"));
            nickname = (String) attributes.get("login");
            avatarUrl = (String) attributes.get("avatar_url");
            email = (String) attributes.get("email");
        } 
//        else if ("gitee".equalsIgnoreCase(registrationId)) {
//            providerUserId = String.valueOf(attributes.get("id"));
//            nickname = (String) attributes.get("name");
//            avatarUrl = (String) attributes.get("avatar_url");
//            email = (String) attributes.get("email");
//        } 
        else {
            log.error("Unsupported OAuth2 provider: {}", registrationId);
            response.sendRedirect(frontendUrl + "/login?error=unsupported_provider");
            return;
        }

        // Call Service to Register/Login
        Result<UserDTO> result = oAuth2Service.handleLogin(registrationId.toUpperCase(), providerUserId, email, nickname, avatarUrl);

        if (result.getCode() == 200) {
            String token = result.getData().getToken();
            // Redirect to frontend with token
            log.info("OAuth2 login successful, redirecting to frontend with token");
            response.sendRedirect(frontendUrl + "/oauth/callback?token=" + token);
        } else {
            log.error("OAuth2 login failed: {}", result.getMessage());
            response.sendRedirect(frontendUrl + "/login?error=login_failed");
        }
    }
}
