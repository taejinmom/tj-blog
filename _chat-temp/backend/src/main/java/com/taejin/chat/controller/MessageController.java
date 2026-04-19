package com.taejin.chat.controller;

import com.taejin.chat.dto.ChatMessageResponse;
import com.taejin.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final ChatMessageService messageService;

    @GetMapping("/api/chat-rooms/{id}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(messageService.getMessages(id, page, size));
    }

    @PostMapping("/api/messages/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        messageService.markAsRead(id, body.get("userId"));
        return ResponseEntity.ok().build();
    }
}
