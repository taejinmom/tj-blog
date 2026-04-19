package com.taejin.chat.dto;

import com.taejin.chat.entity.ChatRoom;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatRoomResponse {

    private Long id;
    private String name;
    private String type;
    private int memberCount;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom room) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .type(room.getType().name())
                .memberCount(room.getMembers().size())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
