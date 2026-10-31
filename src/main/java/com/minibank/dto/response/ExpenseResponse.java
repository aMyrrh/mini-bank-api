package com.minibank.dto.response;

import com.minibank.entity.Currency;
import com.minibank.entity.TransactionCategory;

import java.math.BigDecimal;

public record ExpenseResponse(
        TransactionCategory category,
        Currency currency,
        BigDecimal total,
        long count
) {
}
