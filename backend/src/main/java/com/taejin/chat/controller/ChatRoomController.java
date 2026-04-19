package com.taejin.chat.controller;

import com.taejin.chat.dto.*;
import com.taejin.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> findAll() {
        return ResponseEntity.ok(chatRoomService.findAll());
    }

    @PostMapping
    public ResponseEntity<ChatRoomResponse> create(@Valid @RequestBody ChatRoomRequest request) {
        return ResponseEntity.ok(chatRoomService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(chatRoomService.findById(id));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        chatRoomService.join(id, body.get("userId"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserResponse>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(chatRoomService.getMembers(id));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        chatRoomService.leave(id, body.get("userId"));
        return ResponseEntity.ok().build();
    }
}
