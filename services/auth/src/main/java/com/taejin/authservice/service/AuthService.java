package com.taejin.authservice.service;

import com.taejin.authservice.dto.*;
import com.taejin.authservice.entity.RefreshToken;
import com.taejin.authservice.entity.Role;
import com.taejin.authservice.entity.User;
import com.taejin.authservice.exception.ApiException;
import com.taejin.authservice.repository.RefreshTokenRepository;
import com.taejin.authservice.repository.RoleRepository;
import com.taejin.authservice.repository.UserRepository;
import com.taejin.authservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("Email already in use");
        }
        Role userRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(Role.of(DEFAULT_ROLE)));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName())
                .enabled(true)
                .roles(roles)
                .build();
        user = userRepository.save(user);
        log.info("Registered new user id={} email={}", user.getId(), user.getEmail());
        return UserResponse.from(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password())
            );
        } catch (BadCredentialsException e) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        String token = req.refreshToken();
        if (!tokenProvider.isValid(token)
                || !JwtTokenProvider.TYPE_REFRESH.equals(tokenProvider.getType(token))) {
            throw ApiException.unauthorized("Invalid refresh token");
        }
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> ApiException.unauthorized("Refresh token not recognized"));
        if (!stored.isActive()) {
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }
        // 회전(rotation): 기존 토큰 폐기 후 새 토큰 발급
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest req) {
        refreshTokenRepository.revokeByToken(req.refreshToken());
    }

    private TokenResponse issueTokens(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName).collect(Collectors.toList());
        String access = tokenProvider.createAccessToken(user.getId(), user.getEmail(), roles);
        String refresh = tokenProvider.createRefreshToken(user.getId());

        RefreshToken entity = RefreshToken.builder()
                .token(refresh)
                .user(user)
                .expiresAt(Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);

        return TokenResponse.bearer(access, refresh);
    }
}
