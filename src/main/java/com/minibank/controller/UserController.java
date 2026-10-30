package com.minibank.controller;

import com.minibank.dto.request.UpdateUserRequest;
import com.minibank.dto.response.UserResponse;
import com.minibank.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal Long userId) {
        return userService.getById(userId);
    }

    @PatchMapping("/me")
    public UserResponse updateCurrentUser(@AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateFullName(userId, request);
    }
}
