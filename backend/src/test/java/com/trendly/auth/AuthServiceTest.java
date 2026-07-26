package com.trendly.auth;

import com.trendly.auth.dto.AuthResponse;
import com.trendly.auth.dto.LoginRequest;
import com.trendly.auth.dto.RegisterRequest;
import com.trendly.common.exception.DuplicateFieldException;
import com.trendly.common.exception.InvalidCredentialsException;
import com.trendly.security.JwtService;
import com.trendly.user.User;
import com.trendly.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} (BE-2.1 register, BE-2.2 login),
 * mocking {@link UserRepository}, {@link PasswordEncoder}, and
 * {@link JwtService} so the DB conflict-mapping and credential-checking
 * logic is exercised without a real database. DB-backed behavior (actual
 * unique index violations, bcrypt hash persistence) is covered separately
 * by the integration tests in {@code AuthIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_success_hashesPasswordAndIssuesToken() {
        RegisterRequest request = new RegisterRequest("ann@example.com", "ann", "password123", "Ann");
        when(userRepository.existsByEmailIgnoreCase("ann@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("ann")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", 1L);
            return toSave;
        });
        when(jwtService.issueToken(1L)).thenReturn("signed-jwt");

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("signed-jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().username()).isEqualTo("ann");
        assertThat(response.user().email()).isEqualTo("ann@example.com");
        assertThat(response.user().displayName()).isEqualTo("Ann");

        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void register_duplicateEmail_preCheckThrowsBeforeSaving() {
        RegisterRequest request = new RegisterRequest("ann@example.com", "ann", "password123", null);
        when(userRepository.existsByEmailIgnoreCase("ann@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateFieldException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_duplicateUsername_preCheckThrowsBeforeSaving() {
        RegisterRequest request = new RegisterRequest("ann@example.com", "ann", "password123", null);
        when(userRepository.existsByEmailIgnoreCase("ann@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("ann")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateFieldException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_raceLostToConcurrentEmailInsert_mapsIndexNameTo409Message() {
        // Simulates the race window: pre-check passes, but the DB unique
        // index still rejects the insert (another request won the race).
        // Postgres's error references the index name, not a plain column.
        RegisterRequest request = new RegisterRequest("ann@example.com", "ann", "password123", null);
        when(userRepository.existsByEmailIgnoreCase("ann@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("ann")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        RuntimeException pgCause = new RuntimeException(
                "duplicate key value violates unique constraint \"uq_users_email_lower\"");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("insert failed", pgCause));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateFieldException.class)
                .hasMessageContaining("email");
    }

    @Test
    void register_raceLostToConcurrentUsernameInsert_mapsIndexNameTo409Message() {
        RegisterRequest request = new RegisterRequest("ann@example.com", "ann", "password123", null);
        when(userRepository.existsByEmailIgnoreCase("ann@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("ann")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        RuntimeException pgCause = new RuntimeException(
                "duplicate key value violates unique constraint \"uq_users_username_lower\"");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("insert failed", pgCause));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateFieldException.class)
                .hasMessageContaining("username");
    }

    @Test
    void login_correctCredentials_issuesToken() {
        User user = new User("ann@example.com", "ann", "hashed-password", "Ann");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmailOrUsernameIgnoreCase("ann@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.issueToken(1L)).thenReturn("signed-jwt");

        AuthResponse response = authService.login(new LoginRequest("ann@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("signed-jwt");
        assertThat(response.user().username()).isEqualTo("ann");
    }

    @Test
    void login_byUsername_alsoResolves() {
        User user = new User("ann@example.com", "ann", "hashed-password", "Ann");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmailOrUsernameIgnoreCase("ann")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.issueToken(1L)).thenReturn("signed-jwt");

        AuthResponse response = authService.login(new LoginRequest("ann", "password123"));

        assertThat(response.user().email()).isEqualTo("ann@example.com");
    }

    @Test
    void login_unknownIdentifier_throwsInvalidCredentials() {
        when(userRepository.findByEmailOrUsernameIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials_sameAsUnknownUser() {
        User user = new User("ann@example.com", "ann", "hashed-password", "Ann");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmailOrUsernameIgnoreCase("ann@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("wrong-password"), eq("hashed-password"))).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ann@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtService, never()).issueToken(any());
    }
}
