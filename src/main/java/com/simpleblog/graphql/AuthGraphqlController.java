package com.simpleblog.graphql;

import com.simpleblog.model.dto.AuthPayload;
import com.simpleblog.model.dto.LoginInput;
import com.simpleblog.model.entity.User;
import com.simpleblog.security.SecurityUtils;
import com.simpleblog.service.AuthService;
import com.simpleblog.service.UserService;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.stereotype.Controller;

@Controller
public class AuthGraphqlController {
    private final AuthService authService;
    private final UserService userService;

    public AuthGraphqlController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @MutationMapping
    public AuthPayload login(@Argument LoginInput input) {
        return authService.login(input.username(), input.password());
    }

    @QueryMapping
    public User me() {
        return SecurityUtils.currentUsername()
                .map(userService::findByUsername)
                .orElse(null);
    }
}
