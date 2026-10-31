package com.minibank.service;

import com.minibank.dto.response.ExpenseResponse;
import com.minibank.dto.response.PageResponse;
import com.minibank.dto.response.StatementResponse;
import com.minibank.dto.response.TransactionResponse;
import com.minibank.entity.Account;
import com.minibank.entity.TransactionType;
import com.minibank.exception.BusinessRuleException;
import com.minibank.exception.NotFoundException;
import com.minibank.mapper.TransactionMapper;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.AccountTurnover;
import com.minibank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getHistory(Long userId, Long accountId, TransactionType type,
                                                        LocalDate from, LocalDate to, int page, int size) {
        Account account = findOwnAccount(userId, accountId);
        checkPeriod(from, to);

        Instant fromInstant = from != null ? startOfDay(from) : Instant.EPOCH;
        Instant toInstant = to != null ? startOfDay(to.plusDays(1)) : Instant.now();

        return PageResponse.from(transactionRepository
                .findHistory(account.getId(), type, fromInstant, toInstant, PageRequest.of(page, size))
                .map(transactionMapper::toResponse));
    }

    // Balance and totals come from separate queries: REPEATABLE READ makes them all see the same snapshot
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StatementResponse getStatement(Long userId, Long accountId, LocalDate from, LocalDate to) {
        Account account = findOwnAccount(userId, accountId);
        checkPeriod(from, to);

        Instant periodStart = startOfDay(from);
        Instant periodEnd = startOfDay(to.plusDays(1));

        AccountTurnover sinceStart = transactionRepository.sumTurnover(accountId, periodStart, Instant.now());
        BigDecimal opening = account.getBalance().subtract(sinceStart.getIncome()).add(sinceStart.getOutcome());

        AccountTurnover period = transactionRepository.sumTurnover(accountId, periodStart, periodEnd);
        BigDecimal income = period.getIncome().setScale(2);
        BigDecimal outcome = period.getOutcome().setScale(2);
        BigDecimal closing = opening.add(income).subtract(outcome);

        List<TransactionResponse> transactions = transactionMapper.toResponses(
                transactionRepository.findForStatement(accountId, periodStart, periodEnd));

        return new StatementResponse(account.getAccountNumber(), account.getCurrency(), from, to,
                opening, income, outcome, closing, transactions);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(Long userId, LocalDate from, LocalDate to) {
        checkPeriod(from, to);
        return transactionMapper.toExpenseResponses(
                transactionRepository.findExpensesByCategory(userId, startOfDay(from), startOfDay(to.plusDays(1))));
    }

    private Account findOwnAccount(Long userId, Long accountId) {
        return accountRepository.findByIdAndOwnerId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account " + accountId + " not found"));
    }

    private void checkPeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("Period start must not be after its end");
        }
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
