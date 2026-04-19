package com.taejin.authservice.dto;

import com.taejin.authservice.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        Set<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet())
        );
    }
}
