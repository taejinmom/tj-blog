package com.taejin.authservice.controller;

import com.taejin.authservice.dto.UserResponse;
import com.taejin.authservice.exception.ApiException;
import com.taejin.authservice.repository.UserRepository;
import com.taejin.authservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
