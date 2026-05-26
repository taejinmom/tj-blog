package com.taejin.chat.dto;

import com.taejin.chat.entity.ChatMessage;
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

    // 방 목록용 부가 정보 (사용자 컨텍스트가 있을 때만 채워짐)
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public static ChatRoomResponse from(ChatRoom room) {
        return base(room).build();
    }

    /** 최근 메시지 + 사용자별 안읽음 수를 포함한 응답. */
    public static ChatRoomResponse withMeta(ChatRoom room, ChatMessage lastMessage, long unreadCount) {
        return base(room)
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .unreadCount(unreadCount)
                .build();
    }

    private static ChatRoomResponseBuilder base(ChatRoom room) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .type(room.getType().name())
                .memberCount(room.getMembers().size())
                .createdAt(room.getCreatedAt());
    }
}
