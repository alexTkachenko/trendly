package com.trendly.backend.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByAuthor_UsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE p.author.id IN (
                SELECT f.followee.id FROM Follow f WHERE f.follower.username = :username
            )
            ORDER BY p.createdAt DESC
            """)
    Page<Post> findFeedForUser(@Param("username") String username, Pageable pageable);
}
