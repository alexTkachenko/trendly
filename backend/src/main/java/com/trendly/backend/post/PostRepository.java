package com.trendly.backend.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

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

    boolean existsByAuthor_UsernameAndOriginalPost_Id(String username, Long originalPostId);

    Optional<Post> findByAuthor_UsernameAndOriginalPost_Id(String username, Long originalPostId);

    @Transactional
    void deleteByOriginalPost_Id(Long originalPostId);

    @Query("SELECT p.originalPost.id FROM Post p WHERE p.author.username = :username AND p.originalPost IS NOT NULL")
    Set<Long> findRepostedOriginalPostIds(@Param("username") String username);
}
