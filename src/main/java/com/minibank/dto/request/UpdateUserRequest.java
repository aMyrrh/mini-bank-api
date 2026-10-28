package com.minibank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 255) String fullName
) {
}
