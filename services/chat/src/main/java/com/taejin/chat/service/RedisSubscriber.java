package com.taejin.chat.service;

import com.taejin.chat.dto.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            ChatMessageResponse chatMessage = objectMapper.readValue(body, ChatMessageResponse.class);
            messagingTemplate.convertAndSend(
                    "/topic/chat-room/" + chatMessage.getRoomId(),
                    chatMessage
            );
        } catch (Exception e) {
            log.error("Failed to process Redis message", e);
        }
    }
}
