package com.minibank.dto.response;

import com.minibank.entity.Currency;
import com.minibank.entity.TransactionCategory;
import com.minibank.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        TransactionType type,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        Currency currency,
        TransactionCategory category,
        String description,
        Instant createdAt
) {
}
