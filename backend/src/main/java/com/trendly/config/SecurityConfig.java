package com.trendly.config;

import com.trendly.security.JwtAuthenticationFilter;
import com.trendly.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security skeleton (BE-1.1) extended with the stateless JWT filter
 * (BE-1.3) and, as of BE-2.1/BE-2.2, the unauthenticated auth endpoints and
 * the shared password-hashing bean. {@link JwtAuthenticationFilter}
 * populates the security context from a valid bearer token if one is
 * present; it never rejects a request itself, so
 * {@code /actuator/health}, {@code /auth/register}, and {@code /auth/login}
 * stay reachable unauthenticated while every other route requires a valid
 * token.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService);
    }

    /**
     * Shared password-hashing bean (BE-2.1) -- BCrypt, used by
     * {@code AuthService} to hash on register and verify on login. No other
     * {@link PasswordEncoder} bean should be defined anywhere else in the
     * app.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> {})
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
