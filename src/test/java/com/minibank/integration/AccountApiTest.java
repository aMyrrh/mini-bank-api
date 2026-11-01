package com.minibank.integration;

import com.minibank.dto.response.AccountResponse;
import com.minibank.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountApiTest extends AbstractIntegrationTest {

    private Long userId;
    private String token;

    @BeforeEach
    void setUp() {
        String email = uniqueEmail();
        userId = createUser(email);
        token = bearerToken(email);
    }

    @Test
    void openAccountReturnsCreatedWithLocation() throws Exception {
        mvc.perform(post("/api/accounts").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency": "USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/accounts/")))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andExpect(jsonPath("$.accountNumber").value(matchesPattern("40817840\\d{12}")));
    }

    @Test
    void unsupportedCurrencyReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/accounts").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency": "GBP"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listContainsOnlyOwnAccounts() throws Exception {
        AccountResponse own = openAccount(userId, Currency.RUB);
        openAccount(createUser(), Currency.RUB);

        mvc.perform(get("/api/accounts").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(own.id()));
    }

    @Test
    void foreignAccountReturnsNotFound() throws Exception {
        AccountResponse foreign = openAccount(createUser(), Currency.RUB);

        mvc.perform(get("/api/accounts/" + foreign.id()).header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/accounts/" + foreign.id() + "/close").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void emptyAccountCanBeClosedOnlyOnce() throws Exception {
        AccountResponse account = openAccount(userId, Currency.RUB);

        mvc.perform(post("/api/accounts/" + account.id() + "/close").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(post("/api/accounts/" + account.id() + "/close").header("Authorization", token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void accountWithMoneyCannotBeClosed() throws Exception {
        AccountResponse account = openAccount(userId, Currency.RUB);
        deposit(userId, account.id(), "10.00");

        mvc.perform(post("/api/accounts/" + account.id() + "/close").header("Authorization", token))
                .andExpect(status().isUnprocessableEntity());
    }
}
