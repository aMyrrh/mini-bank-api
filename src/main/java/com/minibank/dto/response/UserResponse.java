package com.minibank.dto.response;

import com.minibank.entity.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        Instant createdAt
) {
}
