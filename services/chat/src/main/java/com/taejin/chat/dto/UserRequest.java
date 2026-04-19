package com.taejin.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UserRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String nickname;

    @NotBlank
    private String password;
}
