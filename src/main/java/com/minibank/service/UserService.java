package com.minibank.service;

import com.minibank.dto.request.CreateUserRequest;
import com.minibank.dto.request.UpdateUserRequest;
import com.minibank.dto.response.UserResponse;
import com.minibank.entity.Role;
import com.minibank.entity.User;
import com.minibank.exception.ConflictException;
import com.minibank.exception.NotFoundException;
import com.minibank.mapper.UserMapper;
import com.minibank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(CreateUserRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(), Role.USER);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateFullName(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        user.changeFullName(request.fullName().trim());
        return userMapper.toResponse(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
    }
}
