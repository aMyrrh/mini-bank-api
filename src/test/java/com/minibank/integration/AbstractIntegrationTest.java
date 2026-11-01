package com.minibank.integration;

import com.minibank.dto.request.CreateAccountRequest;
import com.minibank.dto.request.CreateUserRequest;
import com.minibank.dto.request.DepositRequest;
import com.minibank.dto.request.LoginRequest;
import com.minibank.dto.response.AccountResponse;
import com.minibank.entity.Currency;
import com.minibank.service.AccountService;
import com.minibank.service.AuthService;
import com.minibank.service.TransactionService;
import com.minibank.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "password123";

    // One container for all test classes: started once, removed by Testcontainers when the JVM exits
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UserService userService;
    @Autowired
    protected AuthService authService;
    @Autowired
    protected AccountService accountService;
    @Autowired
    protected TransactionService transactionService;

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected Long createUser(String email) {
        return userService.register(new CreateUserRequest(email, PASSWORD, "Test User")).id();
    }

    protected Long createUser() {
        return createUser(uniqueEmail());
    }

    protected String bearerToken(String email) {
        return "Bearer " + authService.login(new LoginRequest(email, PASSWORD)).accessToken();
    }

    protected AccountResponse openAccount(Long userId, Currency currency) {
        return accountService.open(userId, new CreateAccountRequest(currency));
    }

    protected void deposit(Long userId, Long accountId, String amount) {
        transactionService.deposit(userId, accountId, new DepositRequest(new BigDecimal(amount), null), UUID.randomUUID());
    }

    protected BigDecimal balance(Long userId, Long accountId) {
        return accountService.getById(userId, accountId).balance();
    }
}
