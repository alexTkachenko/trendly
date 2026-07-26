package com.trendly.backend.post;

import com.trendly.backend.common.ForbiddenException;
import com.trendly.backend.common.MediaStorageService;
import com.trendly.backend.common.NotFoundException;
import com.trendly.backend.common.PageResponse;
import com.trendly.backend.user.User;
import com.trendly.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MediaStorageService mediaStorageService;

    public PostController(PostRepository postRepository, UserRepository userRepository, MediaStorageService mediaStorageService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping("/posts")
    public ResponseEntity<PostResponse> create(
            @RequestParam String text,
            @RequestParam(required = false) MultipartFile image,
            Authentication authentication
    ) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }

        User author = currentUser(authentication);

        Post post = new Post();
        post.setAuthor(author);
        post.setTextContent(text);
        if (image != null && !image.isEmpty()) {
            post.setImageUrl(mediaStorageService.store(image));
        }

        Post saved = postRepository.save(post);
        return ResponseEntity.ok(PostResponse.from(saved));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<PostResponse> getById(@PathVariable Long id, Authentication authentication) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        Long originalId = post.getOriginalPost() != null ? post.getOriginalPost().getId() : post.getId();
        boolean repostedByMe = postRepository.existsByAuthor_UsernameAndOriginalPost_Id(authentication.getName(), originalId);
        return ResponseEntity.ok(PostResponse.from(post, repostedByMe));
    }

    @GetMapping("/feed")
    public ResponseEntity<PageResponse<PostResponse>> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Page<Post> posts = postRepository.findFeedForUser(authentication.getName(), PageRequest.of(page, size));
        Set<Long> repostedIds = postRepository.findRepostedOriginalPostIds(authentication.getName());
        return ResponseEntity.ok(PageResponse.of(posts, post -> toResponse(post, repostedIds)));
    }

    @GetMapping("/users/{username}/posts")
    public ResponseEntity<PageResponse<PostResponse>> getByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        if (!userRepository.existsByUsername(username)) {
            throw new NotFoundException("User not found");
        }
        Page<Post> posts = postRepository.findByAuthor_UsernameOrderByCreatedAtDesc(username, PageRequest.of(page, size));
        Set<Long> repostedIds = postRepository.findRepostedOriginalPostIds(authentication.getName());
        return ResponseEntity.ok(PageResponse.of(posts, post -> toResponse(post, repostedIds)));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Post not found"));

        if (!post.getAuthor().getUsername().equals(authentication.getName())) {
            throw new ForbiddenException("You can only delete your own posts");
        }

        // Deleting an original also removes everyone's reposts of it; no-op if
        // `post` is itself a repost (nothing reposts a repost).
        postRepository.deleteByOriginalPost_Id(id);

        if (post.getImageUrl() != null) {
            mediaStorageService.delete(post.getImageUrl());
        }
        postRepository.delete(post);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private PostResponse toResponse(Post post, Set<Long> repostedByMeOriginalIds) {
        Long originalId = post.getOriginalPost() != null ? post.getOriginalPost().getId() : post.getId();
        return PostResponse.from(post, repostedByMeOriginalIds.contains(originalId));
    }
}
