package com.taejin.chat.dto;

import com.taejin.chat.entity.ChatMessage;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderNickname;
    private String content;
    private String type;
    private LocalDateTime createdAt;
    private int unreadCount;

    public static ChatMessageResponse from(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .roomId(msg.getChatRoom().getId())
                .senderId(msg.getSender().getId())
                .senderNickname(msg.getSender().getNickname())
                .content(msg.getContent())
                .type(msg.getType().name())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    public static ChatMessageResponse from(ChatMessage msg, int unreadCount) {
        ChatMessageResponse response = from(msg);
        response.setUnreadCount(unreadCount);
        return response;
    }
}
