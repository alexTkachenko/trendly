package com.trendly.backend.follow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollower_UsernameAndFollowee_Username(String followerUsername, String followeeUsername);

    Optional<Follow> findByFollower_UsernameAndFollowee_Username(String followerUsername, String followeeUsername);
}
