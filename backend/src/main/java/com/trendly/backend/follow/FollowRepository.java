package com.trendly.backend.follow;

import com.trendly.backend.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollower_UsernameAndFollowee_Username(String followerUsername, String followeeUsername);

    Optional<Follow> findByFollower_UsernameAndFollowee_Username(String followerUsername, String followeeUsername);

    @Query("SELECT f.followee FROM Follow f WHERE f.follower.username = :username ORDER BY f.createdAt DESC")
    Page<User> findFolloweesByUsername(@Param("username") String username, Pageable pageable);
}
