package com.trendly.backend.user;

import com.trendly.backend.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationController {

    private final UserRepository userRepository;

    public RecommendationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<PageResponse<UserSummaryDto>> recommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication
    ) {
        Page<User> users = userRepository.findRecommendations(authentication.getName(), PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(users, UserSummaryDto::from));
    }
}
