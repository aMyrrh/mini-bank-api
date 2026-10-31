package com.minibank.controller;

import com.minibank.dto.response.ExpenseResponse;
import com.minibank.dto.response.PageResponse;
import com.minibank.dto.response.StatementResponse;
import com.minibank.dto.response.TransactionResponse;
import com.minibank.entity.TransactionType;
import com.minibank.service.ReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/api/accounts/{id}/transactions")
    public PageResponse<TransactionResponse> getHistory(@AuthenticationPrincipal Long userId,
                                                        @PathVariable Long id,
                                                        @RequestParam(required = false) TransactionType type,
                                                        @RequestParam(required = false) LocalDate from,
                                                        @RequestParam(required = false) LocalDate to,
                                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reportService.getHistory(userId, id, type, from, to, page, size);
    }

    @GetMapping("/api/accounts/{id}/statement")
    public StatementResponse getStatement(@AuthenticationPrincipal Long userId,
                                          @PathVariable Long id,
                                          @RequestParam LocalDate from,
                                          @RequestParam LocalDate to) {
        return reportService.getStatement(userId, id, from, to);
    }

    @GetMapping("/api/analytics/expenses")
    public List<ExpenseResponse> getExpenses(@AuthenticationPrincipal Long userId,
                                             @RequestParam LocalDate from,
                                             @RequestParam LocalDate to) {
        return reportService.getExpenses(userId, from, to);
    }
}
