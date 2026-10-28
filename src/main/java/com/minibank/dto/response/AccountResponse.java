package com.minibank.dto.response;

import com.minibank.entity.AccountStatus;
import com.minibank.entity.Currency;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String accountNumber,
        Currency currency,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt
) {
}
