package com.trendly.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository over {@code users}.
 *
 * Every lookup here filters on {@code lower(...)} explicitly (JPQL
 * {@code lower(u.email)} compiles to SQL {@code lower(email)}) per
 * {@code docs/db/conventions.md}, rather than relying on Spring Data's
 * derived {@code IgnoreCase} keyword -- that translates to {@code UPPER(...)}
 * comparisons on some providers, which would not hit the
 * {@code uq_users_email_lower}/{@code uq_users_username_lower} indexes
 * (built on {@code lower(...)}, not {@code upper(...)}).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT (COUNT(u) > 0) FROM User u WHERE lower(u.email) = lower(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT (COUNT(u) > 0) FROM User u WHERE lower(u.username) = lower(:username)")
    boolean existsByUsernameIgnoreCase(@Param("username") String username);

    /**
     * BE-2.2's login lookup: {@code login} may be an email or a username,
     * case-insensitive either way.
     */
    @Query("SELECT u FROM User u WHERE lower(u.email) = lower(:login) OR lower(u.username) = lower(:login)")
    Optional<User> findByEmailOrUsernameIgnoreCase(@Param("login") String login);
}
