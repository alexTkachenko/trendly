package com.trendly.backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query(value = """
            SELECT u FROM User u
            WHERE u.username <> :username
            AND u.username NOT IN (
                SELECT f.followee.username FROM Follow f WHERE f.follower.username = :username
            )
            ORDER BY (
                SELECT COUNT(f2) FROM Follow f2 WHERE f2.followee = u
            ) DESC, u.id ASC
            """,
            countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE u.username <> :username
            AND u.username NOT IN (
                SELECT f.followee.username FROM Follow f WHERE f.follower.username = :username
            )
            """)
    Page<User> findRecommendations(@Param("username") String username, Pageable pageable);
}
