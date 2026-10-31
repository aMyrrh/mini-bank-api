package com.minibank.dto.response;

import com.minibank.entity.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatementResponse(
        String accountNumber,
        Currency currency,
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,
        BigDecimal totalIncome,
        BigDecimal totalOutcome,
        BigDecimal closingBalance,
        List<TransactionResponse> transactions
) {
}
