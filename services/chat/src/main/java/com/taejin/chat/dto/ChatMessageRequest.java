package com.taejin.chat.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ChatMessageRequest {

    private Long roomId;
    private Long senderId;
    private String content;
    private String type = "TEXT";
}
