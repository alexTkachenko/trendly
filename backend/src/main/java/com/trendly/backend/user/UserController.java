package com.trendly.backend.user;

import com.trendly.backend.common.NotFoundException;
import com.trendly.backend.common.PageResponse;
import com.trendly.backend.follow.FollowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public UserController(UserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return ResponseEntity.ok(MeResponse.from(user));
    }

    @GetMapping("/{username}/following")
    public ResponseEntity<PageResponse<UserSummaryDto>> following(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (!userRepository.existsByUsername(username)) {
            throw new NotFoundException("User not found");
        }
        Page<User> followees = followRepository.findFolloweesByUsername(username, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(followees, UserSummaryDto::from));
    }
}
