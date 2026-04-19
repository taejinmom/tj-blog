package com.taejin.chat.dto;

import com.taejin.chat.entity.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String nickname;
    private String status;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
