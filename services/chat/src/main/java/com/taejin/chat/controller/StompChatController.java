package com.taejin.chat.controller;

import com.taejin.chat.dto.ChatMessageRequest;
import com.taejin.chat.dto.ChatMessageResponse;
import com.taejin.chat.service.ChatMessageService;
import com.taejin.chat.service.RedisPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class StompChatController {

    private final ChatMessageService messageService;
    private final RedisPublisher redisPublisher;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());
        request.setSenderId(senderId);
        ChatMessageResponse response = messageService.save(request);
        redisPublisher.publish(response);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> payload) {
        // Typing indicator - handled by the frontend via subscription
    }
}
