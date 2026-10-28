package com.minibank.controller;

import com.minibank.dto.request.CreateUserRequest;
import com.minibank.dto.request.UpdateUserRequest;
import com.minibank.dto.response.UserResponse;
import com.minibank.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CreateUserRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        return userService.getById(userId);
    }

    @PatchMapping("/me")
    public UserResponse updateCurrentUser(@RequestHeader("X-User-Id") Long userId,
                                          @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateFullName(userId, request);
    }
}
