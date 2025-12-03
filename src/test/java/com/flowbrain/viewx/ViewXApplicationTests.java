package com.flowbrain.viewx;

import com.flowbrain.viewx.common.Role;
import com.flowbrain.viewx.common.UserStatus;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.service.AuthenticationService;
import com.flowbrain.viewx.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

@SpringBootTest
class ViewXApplicationTests {

    @Autowired
    UserService userService;

    @Autowired
    AuthenticationService authenticationService;

    @Test
    void insertSuperAdmin() {

    }

}
