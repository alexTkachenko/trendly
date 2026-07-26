package com.trendly.backend.comment;

import com.trendly.backend.common.NotFoundException;
import com.trendly.backend.common.PageResponse;
import com.trendly.backend.post.Post;
import com.trendly.backend.post.PostRepository;
import com.trendly.backend.user.User;
import com.trendly.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentController(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        User author = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(request.content());

        Comment saved = commentRepository.save(comment);
        return ResponseEntity.ok(CommentResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CommentResponse>> list(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("Post not found");
        }
        Page<Comment> comments = commentRepository.findByPost_IdOrderByCreatedAtAsc(postId, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(comments, CommentResponse::from));
    }
}
