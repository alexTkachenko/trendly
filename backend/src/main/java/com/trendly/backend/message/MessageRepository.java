package com.trendly.backend.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            WHERE (m.sender.username = :a AND m.recipient.username = :b)
               OR (m.sender.username = :b AND m.recipient.username = :a)
            ORDER BY m.createdAt DESC
            """)
    Page<Message> findConversation(@Param("a") String a, @Param("b") String b, Pageable pageable);

    @Query("""
            SELECT m FROM Message m
            WHERE m.sender.username = :username OR m.recipient.username = :username
            ORDER BY m.createdAt DESC
            """)
    List<Message> findAllInvolvingOrderByCreatedAtDesc(@Param("username") String username);
}
