package com.flowbrain.viewx;

import com.flowbrain.viewx.service.AuthenticationService;
import com.flowbrain.viewx.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
