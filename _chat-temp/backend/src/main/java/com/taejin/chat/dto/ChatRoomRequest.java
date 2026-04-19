package com.taejin.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ChatRoomRequest {

    @NotBlank
    private String name;

    private String type = "GROUP";

    private Long creatorId;
}
