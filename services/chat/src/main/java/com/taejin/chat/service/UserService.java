package com.taejin.chat.service;

import com.taejin.chat.dto.*;
import com.taejin.chat.entity.User;
import com.taejin.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse register(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        User user = User.builder()
                .username(request.getUsername())
                .nickname(request.getNickname())
                .password(request.getPassword())
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
        user.setStatus(User.UserStatus.ONLINE);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id)));
    }

    @Transactional
    public void updateStatus(Long userId, User.UserStatus status) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
    }
}
