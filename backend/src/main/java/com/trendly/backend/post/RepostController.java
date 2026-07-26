package com.trendly.backend.post;

import com.trendly.backend.common.NotFoundException;
import com.trendly.backend.user.User;
import com.trendly.backend.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public RepostController(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/posts/{id}/repost")
    public ResponseEntity<RepostStatusResponse> repost(@PathVariable Long id, Authentication authentication) {
        Post original = resolveOriginal(id);
        String me = authentication.getName();

        if (!postRepository.existsByAuthor_UsernameAndOriginalPost_Id(me, original.getId())) {
            User user = userRepository.findByUsername(me)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            Post repost = new Post();
            repost.setAuthor(user);
            repost.setOriginalPost(original);
            postRepository.save(repost);
        }

        return ResponseEntity.ok(new RepostStatusResponse(true));
    }

    @DeleteMapping("/posts/{id}/repost")
    public ResponseEntity<RepostStatusResponse> undoRepost(@PathVariable Long id, Authentication authentication) {
        Post original = resolveOriginal(id);
        postRepository.findByAuthor_UsernameAndOriginalPost_Id(authentication.getName(), original.getId())
                .ifPresent(postRepository::delete);
        return ResponseEntity.ok(new RepostStatusResponse(false));
    }

    private Post resolveOriginal(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        return post.getOriginalPost() != null ? post.getOriginalPost() : post;
    }
}
