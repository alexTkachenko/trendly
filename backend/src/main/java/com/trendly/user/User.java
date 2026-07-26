package com.trendly.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Maps the {@code users} table (DB-2.1, see {@code docs/db/schema.md}).
 *
 * {@code created_at}/{@code updated_at} are DB-owned ({@code DEFAULT now()}
 * plus {@code trg_users_set_updated_at}) -- marked {@code insertable = false,
 * updatable = false} so Hibernate never writes them itself; the DB always
 * wins. {@code ddl-auto=validate} means this mapping must match the Flyway
 * schema column-for-column, not the other way around.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "bio")
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected User() {
        // JPA
    }

    /**
     * Registration constructor -- only the columns BE-2.1's register
     * endpoint actually sets. {@code bio}/{@code avatar_url} stay null
     * (nullable columns, no value at registration time; set later by a
     * future profile-edit endpoint, out of this task's scope).
     */
    public User(String email, String username, String passwordHash, String displayName) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
