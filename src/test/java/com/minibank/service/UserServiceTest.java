package com.minibank.service;

import com.minibank.dto.request.CreateUserRequest;
import com.minibank.entity.Role;
import com.minibank.entity.User;
import com.minibank.exception.ConflictException;
import com.minibank.mapper.UserMapper;
import com.minibank.mapper.UserMapperImpl;
import com.minibank.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Spy
    private UserMapper userMapper = new UserMapperImpl();
    @InjectMocks
    private UserService userService;

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        userService.register(new CreateUserRequest("Ivan@Example.COM", "password123", " Ivan Petrov "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ivan@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getValue().getFullName()).isEqualTo("Ivan Petrov");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerWithTakenEmailFails() {
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(
                new CreateUserRequest("ivan@example.com", "password123", "Ivan Petrov")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }
}
