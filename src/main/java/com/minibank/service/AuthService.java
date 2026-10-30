package com.minibank.service;

import com.minibank.dto.request.LoginRequest;
import com.minibank.dto.response.TokenResponse;
import com.minibank.entity.User;
import com.minibank.exception.UnauthorizedException;
import com.minibank.repository.UserRepository;
import com.minibank.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase(Locale.ROOT))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return new TokenResponse(jwtService.generateToken(user), "Bearer", jwtService.getExpirationSeconds());
    }
}
