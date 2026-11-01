package com.minibank.security;

import com.minibank.entity.Role;
import com.minibank.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = secret("first-secret-for-tests-32-bytes!");
    private static final String OTHER_SECRET = secret("other-secret-for-tests-32-bytes!");

    private final JwtService jwtService = new JwtService(SECRET, 60);
    private final User user = new User("ivan@example.com", "hash", "Ivan Petrov", Role.ADMIN);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(user, "id", 42L);
    }

    @Test
    void generatedTokenContainsUserIdAndRole() {
        Claims claims = jwtService.parseToken(jwtService.generateToken(user));

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService(OTHER_SECRET, 60).generateToken(user);

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        String token = new JwtService(SECRET, -1).generateToken(user);

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> jwtService.parseToken("not-a-token")).isInstanceOf(JwtException.class);
    }

    private static String secret(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
