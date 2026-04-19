package com.taejin.chat.service;

import com.taejin.chat.dto.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(ChatMessageResponse message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend("chat", json);
        } catch (Exception e) {
            log.error("Failed to publish message to Redis", e);
        }
    }
}
