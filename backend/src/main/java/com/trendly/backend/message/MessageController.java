package com.trendly.backend.message;

import com.trendly.backend.common.NotFoundException;
import com.trendly.backend.common.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageController(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/conversations")
    public ResponseEntity<PageResponse<ConversationSummary>> conversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        String me = authentication.getName();
        List<Message> all = messageRepository.findAllInvolvingOrderByCreatedAtDesc(me);

        // Already ordered newest-first, so the first message we see per partner is their latest.
        Map<String, Message> latestByPartner = new LinkedHashMap<>();
        for (Message message : all) {
            String partner = message.getSender().getUsername().equals(me)
                    ? message.getRecipient().getUsername()
                    : message.getSender().getUsername();
            latestByPartner.putIfAbsent(partner, message);
        }

        List<ConversationSummary> summaries = latestByPartner.values().stream()
                .map(message -> ConversationSummary.from(message, me))
                .toList();

        return ResponseEntity.ok(PageResponse.ofList(summaries, page, size));
    }

    @GetMapping("/conversations/{username}/messages")
    public ResponseEntity<PageResponse<MessageResponse>> thread(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        if (!userRepository.existsByUsername(username)) {
            throw new NotFoundException("User not found");
        }
        Page<Message> messages = messageRepository.findConversation(
                authentication.getName(), username, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(messages, MessageResponse::from));
    }

    @PostMapping("/conversations/{username}/messages")
    public ResponseEntity<MessageResponse> send(
            @PathVariable String username,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        User recipient = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        User sender = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(request.content());

        Message saved = messageRepository.save(message);
        return ResponseEntity.ok(MessageResponse.from(saved));
    }
}
