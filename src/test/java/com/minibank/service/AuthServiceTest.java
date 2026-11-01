package com.minibank.service;

import com.minibank.dto.request.LoginRequest;
import com.minibank.dto.response.TokenResponse;
import com.minibank.entity.Role;
import com.minibank.entity.User;
import com.minibank.exception.UnauthorizedException;
import com.minibank.repository.UserRepository;
import com.minibank.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "ivan@example.com";

    private final User user = new User(EMAIL, "hash", "Ivan Petrov", Role.USER);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    @Test
    void loginWithCorrectPasswordReturnsToken() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        TokenResponse response = authService.login(new LoginRequest("Ivan@Example.com", "password123"));

        assertThat(response).isEqualTo(new TokenResponse("token", "Bearer", 3600L));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }
}
