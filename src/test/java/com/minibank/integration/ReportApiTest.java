package com.minibank.integration;

import com.minibank.dto.request.DepositRequest;
import com.minibank.dto.request.TransferRequest;
import com.minibank.dto.response.AccountResponse;
import com.minibank.dto.response.TransactionResponse;
import com.minibank.entity.Currency;
import com.minibank.entity.TransactionCategory;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportApiTest extends AbstractIntegrationTest {

    private static final String TODAY = LocalDate.now(ZoneOffset.UTC).toString();

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long userId;
    private Long otherUserId;
    private String token;
    private AccountResponse account;
    private AccountResponse otherAccount;

    @BeforeEach
    void setUp() {
        String email = uniqueEmail();
        userId = createUser(email);
        otherUserId = createUser();
        token = bearerToken(email);
        account = openAccount(userId, Currency.RUB);
        otherAccount = openAccount(otherUserId, Currency.RUB);
    }

    @Test
    void historySupportsPaginationAndNewestFirst() throws Exception {
        deposit(userId, account.id(), "1.00");
        deposit(userId, account.id(), "2.00");
        deposit(userId, account.id(), "3.00");

        mvc.perform(get(historyUrl() + "?page=0&size=2").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(3.00))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void historyFiltersByTypeAndPeriod() throws Exception {
        deposit(userId, account.id(), "100.00");
        transfer(account.id(), otherAccount.accountNumber(), "10.00", TransactionCategory.GROCERIES);

        mvc.perform(get(historyUrl() + "?type=TRANSFER").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER"));
        mvc.perform(get(historyUrl() + "?from=2000-01-01&to=2000-12-31").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void historyIncludesIncomingTransfers() throws Exception {
        deposit(otherUserId, otherAccount.id(), "50.00");
        transactionService.transfer(otherUserId, new TransferRequest(otherAccount.id(), account.accountNumber(),
                new BigDecimal("5.00"), null, null), UUID.randomUUID());

        mvc.perform(get(historyUrl()).header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fromAccountNumber").value(otherAccount.accountNumber()));
    }

    @Test
    void historyRejectsInvalidParameters() throws Exception {
        mvc.perform(get(historyUrl() + "?size=1000").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get(historyUrl() + "?page=-1").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get(historyUrl() + "?type=NOPE").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get(historyUrl() + "?from=2026-12-31&to=2026-01-01").header("Authorization", token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void historyOfForeignAccountReturnsNotFound() throws Exception {
        mvc.perform(get("/api/accounts/" + otherAccount.id() + "/transactions").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyPageDoesNotTriggerNPlusOne() throws Exception {
        deposit(userId, account.id(), "100.00");
        for (int i = 0; i < 20; i++) {
            AccountResponse target = openAccount(otherUserId, Currency.RUB);
            transfer(account.id(), target.accountNumber(), "1.00", TransactionCategory.OTHER);
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        mvc.perform(get(historyUrl() + "?size=20").header("Authorization", token))
                .andExpect(jsonPath("$.content.length()").value(20));

        // Ownership check + page of transactions + count; would be 20 more without join fetch
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        statistics.setStatisticsEnabled(false);
    }

    @Test
    void statementCalculatesBalances() throws Exception {
        TransactionResponse old = transactionService.deposit(userId, account.id(),
                new DepositRequest(new BigDecimal("1000.00"), null), UUID.randomUUID());
        jdbcTemplate.update("update transactions set created_at = ? where id = ?",
                Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS)), old.id());
        deposit(userId, account.id(), "500.00");
        transfer(account.id(), otherAccount.accountNumber(), "200.50", TransactionCategory.GROCERIES);

        mvc.perform(get(statementUrl(TODAY, TODAY)).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(1000.00))
                .andExpect(jsonPath("$.totalIncome").value(500.00))
                .andExpect(jsonPath("$.totalOutcome").value(200.50))
                .andExpect(jsonPath("$.closingBalance").value(1299.50))
                .andExpect(jsonPath("$.transactions.length()").value(2));
        assertThat(balance(userId, account.id())).isEqualByComparingTo("1299.50");
    }

    @Test
    void statementForPeriodWithoutOperationsKeepsBalance() throws Exception {
        deposit(userId, account.id(), "100.00");
        String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();

        mvc.perform(get(statementUrl(tomorrow, tomorrow)).header("Authorization", token))
                .andExpect(jsonPath("$.openingBalance").value(100.00))
                .andExpect(jsonPath("$.totalIncome").value(0.00))
                .andExpect(jsonPath("$.closingBalance").value(100.00))
                .andExpect(jsonPath("$.transactions.length()").value(0));
    }

    @Test
    void statementRequiresDates() throws Exception {
        mvc.perform(get("/api/accounts/" + account.id() + "/statement").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expensesAreGroupedByCategoryAndCurrency() throws Exception {
        AccountResponse ownSecond = openAccount(userId, Currency.RUB);
        AccountResponse usd = openAccount(userId, Currency.USD);
        AccountResponse otherUsd = openAccount(otherUserId, Currency.USD);
        deposit(userId, account.id(), "1000.00");
        deposit(userId, usd.id(), "100.00");
        transfer(account.id(), otherAccount.accountNumber(), "250.00", TransactionCategory.GROCERIES);
        transfer(account.id(), otherAccount.accountNumber(), "50.00", TransactionCategory.GROCERIES);
        transfer(account.id(), otherAccount.accountNumber(), "100.00", TransactionCategory.TRANSPORT);
        transfer(account.id(), ownSecond.accountNumber(), "300.00", TransactionCategory.SHOPPING);
        transfer(usd.id(), otherUsd.accountNumber(), "10.00", TransactionCategory.GROCERIES);

        mvc.perform(get("/api/analytics/expenses?from=" + TODAY + "&to=" + TODAY).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].category").value("GROCERIES"))
                .andExpect(jsonPath("$[0].currency").value("RUB"))
                .andExpect(jsonPath("$[0].total").value(300.00))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[1].category").value("TRANSPORT"))
                .andExpect(jsonPath("$[2].currency").value("USD"))
                .andExpect(jsonPath("$[2].total").value(10.00));
    }

    private void transfer(Long fromId, String toNumber, String amount, TransactionCategory category) {
        transactionService.transfer(userId, new TransferRequest(fromId, toNumber, new BigDecimal(amount), category, null),
                UUID.randomUUID());
    }

    private String historyUrl() {
        return "/api/accounts/" + account.id() + "/transactions";
    }

    private String statementUrl(String from, String to) {
        return "/api/accounts/" + account.id() + "/statement?from=" + from + "&to=" + to;
    }
}
