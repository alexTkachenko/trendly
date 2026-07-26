package com.trendly.backend.follow;

import com.trendly.backend.common.NotFoundException;
import com.trendly.backend.user.User;
import com.trendly.backend.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{username}/follow")
public class FollowController {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowController(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<FollowStatusResponse> status(@PathVariable String username, Authentication authentication) {
        boolean following = followRepository.existsByFollower_UsernameAndFollowee_Username(authentication.getName(), username);
        return ResponseEntity.ok(new FollowStatusResponse(following));
    }

    @PostMapping
    public ResponseEntity<FollowStatusResponse> follow(@PathVariable String username, Authentication authentication) {
        String me = authentication.getName();
        if (me.equals(username)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        User followee = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!followRepository.existsByFollower_UsernameAndFollowee_Username(me, username)) {
            User follower = userRepository.findByUsername(me)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            Follow follow = new Follow();
            follow.setFollower(follower);
            follow.setFollowee(followee);
            followRepository.save(follow);
        }

        return ResponseEntity.ok(new FollowStatusResponse(true));
    }

    @DeleteMapping
    public ResponseEntity<FollowStatusResponse> unfollow(@PathVariable String username, Authentication authentication) {
        followRepository.findByFollower_UsernameAndFollowee_Username(authentication.getName(), username)
                .ifPresent(followRepository::delete);
        return ResponseEntity.ok(new FollowStatusResponse(false));
    }
}
