package com.simpleblog.graphql;

import com.simpleblog.model.dto.AuthPayload;
import com.simpleblog.model.dto.LoginInput;
import com.simpleblog.model.dto.RegisterInput;
import com.simpleblog.model.entity.User;
import com.simpleblog.security.SecurityUtils;
import com.simpleblog.service.AuthService;
import com.simpleblog.service.UserService;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class AuthGraphqlController {
//    @Autowired
//    private AuthService authService;
//    @Autowired
//    private UserService userService;

    private final AuthService authService;
    private final UserService userService;

    // @Autowired // 省略@Autowired，Spring自动注入
    public AuthGraphqlController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @MutationMapping
    public AuthPayload login(@Argument LoginInput input) {
        return authService.login(input.username(), input.password());
    }

    @MutationMapping
    public AuthPayload register(@Argument RegisterInput input) {
        return authService.register(input.username(), input.password(), input.displayName(), input.email());
    }

    @QueryMapping
    public User me() {
        return SecurityUtils.currentUsername()
                .map(userService::findByUsername)
                .orElse(null);
    }
}
