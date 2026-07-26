package com.trendly.backend.auth;

import com.trendly.backend.common.ConflictException;
import com.trendly.backend.user.User;
import com.trendly.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService("test-secret-test-secret-test-secret-1234", 3_600_000);
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_createsUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("new@example.com", "newuser", "password123", null);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().username()).isEqualTo("newuser");
        assertThat(response.user().email()).isEqualTo("new@example.com");
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "someone", "password123", null);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void register_rejectsDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("fresh@example.com", "taken", "password123", null);
        when(userRepository.existsByEmail("fresh@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_succeedsWithCorrectCredentials() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setUsername("user1");
        user.setPasswordHash("hashed");
        user.setDisplayName("User One");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "correct-password"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().username()).isEqualTo("user1");
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setUsername("user1");
        user.setPasswordHash("hashed");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever1")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
