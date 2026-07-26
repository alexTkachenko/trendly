package com.trendly.backend.post;

import com.trendly.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "posts",
        indexes = @Index(name = "idx_posts_author_created", columnList = "author_id, created_at"),
        uniqueConstraints = @UniqueConstraint(name = "uq_posts_author_original", columnNames = {"author_id", "original_post_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Null for a regular post; required text for one. */
    @Column(name = "text_content", columnDefinition = "text")
    private String textContent;

    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Set only on a repost "stub": a post row whose author is the reposter and
     * which carries no content of its own — its text/image are always read from
     * the referenced original. Null for a regular, original post. Always points
     * at a true original (never at another repost), so reposting a repost just
     * creates another stub pointing at the same original.
     */
    @ManyToOne
    @JoinColumn(name = "original_post_id")
    private Post originalPost;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
