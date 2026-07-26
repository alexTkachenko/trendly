package com.trendly.auth;

import com.trendly.auth.dto.AuthResponse;
import com.trendly.auth.dto.LoginRequest;
import com.trendly.auth.dto.RegisterRequest;
import com.trendly.auth.dto.UserSummary;
import com.trendly.common.exception.DuplicateFieldException;
import com.trendly.common.exception.InvalidCredentialsException;
import com.trendly.security.JwtService;
import com.trendly.user.User;
import com.trendly.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Registration (BE-2.1) and login (BE-2.2) business logic.
 *
 * Uniqueness for {@code email}/{@code username} is ultimately enforced by
 * the DB's {@code uq_users_email_lower}/{@code uq_users_username_lower}
 * indexes (DB-2.1) -- the pre-checks below are a best-effort UX layer to
 * report *which* field conflicted without inspecting the DB error, per
 * {@code docs/db/conventions.md} ("a race-prone check-then-insert is never
 * load-bearing"). The {@code saveAndFlush} + {@code catch} around it is the
 * actual safety net for the race window between the pre-check and the
 * insert (two concurrent registrations for the same email), where the
 * conflict surfaces as a {@link DataIntegrityViolationException} whose root
 * cause names the violated index, not a plain column name.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateFieldException("email is already registered");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateFieldException("username is already taken");
        }

        User user = new User(
                request.email(),
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName());

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateFieldExceptionFrom(ex);
        }

        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailOrUsernameIgnoreCase(request.login())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.issueToken(user.getId());
        UserSummary summary = new UserSummary(
                user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl());
        return new AuthResponse(accessToken, "Bearer", summary);
    }

    /**
     * The unique-violation root cause message from Postgres references the
     * *index* name ({@code uq_users_email_lower}), not a plain column name
     * -- see {@code docs/db/schema.md}'s note on this. Falls back to a
     * generic message if neither known index name is found (shouldn't
     * happen given the current schema, but avoids swallowing an unrelated
     * constraint violation silently).
     */
    private DuplicateFieldException duplicateFieldExceptionFrom(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("uq_users_email_lower")) {
            return new DuplicateFieldException("email is already registered");
        }
        if (detail != null && detail.contains("uq_users_username_lower")) {
            return new DuplicateFieldException("username is already taken");
        }
        return new DuplicateFieldException("email or username is already taken");
    }
}
