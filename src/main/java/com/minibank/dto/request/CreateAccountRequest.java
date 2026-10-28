package com.minibank.dto.request;

import com.minibank.entity.Currency;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull Currency currency
) {
}
