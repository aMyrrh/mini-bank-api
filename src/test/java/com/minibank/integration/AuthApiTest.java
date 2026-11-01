package com.minibank.integration;

import com.minibank.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends AbstractIntegrationTest {

    @Test
    void registerThenLoginThenGetProfile() throws Exception {
        String email = uniqueEmail();

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "fullName": "Ivan Petrov"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        mvc.perform(get("/api/users/me").header("Authorization", bearerToken(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "short", "fullName": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String email = uniqueEmail();
        createUser(email);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "fullName": "Ivan Petrov"}
                                """.formatted(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        createUser(email);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "wrong-password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void requestWithoutTokenReturnsUnauthorizedInOurFormat() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void requestWithInvalidTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointIsForbiddenForRegularUser() throws Exception {
        String email = uniqueEmail();
        createUser(email);

        mvc.perform(get("/api/admin/users").header("Authorization", bearerToken(email)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanListUsers() throws Exception {
        mvc.perform(get("/api/admin/users").header("Authorization", bearerToken("admin@minibank.local", "admin12345")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'admin@minibank.local')].role").value("ADMIN"));
    }

    private String bearerToken(String email, String password) {
        return "Bearer " + authService.login(new LoginRequest(email, password)).accessToken();
    }
}
