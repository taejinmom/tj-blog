package com.taejin.authservice.service;

import com.taejin.authservice.dto.TokenResponse;
import com.taejin.authservice.entity.RefreshToken;
import com.taejin.authservice.entity.Role;
import com.taejin.authservice.entity.User;
import com.taejin.authservice.entity.UserIdentity;
import com.taejin.authservice.exception.ApiException;
import com.taejin.authservice.repository.RefreshTokenRepository;
import com.taejin.authservice.repository.RoleRepository;
import com.taejin.authservice.repository.UserIdentityRepository;
import com.taejin.authservice.repository.UserRepository;
import com.taejin.authservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2 소셜 로그인 시 사용자 프로비저닝과 토큰 발급을 담당한다.
 *
 * 흐름:
 * 1. (provider, providerUserId)로 기존 UserIdentity 조회 → 있으면 연결된 User 반환.
 * 2. 없고 email이 기존 로컬 User와 일치하면 자동 연결(UserIdentity만 추가).
 * 3. 둘 다 아니면 새 User(비밀번호 없음, ROLE_USER) + UserIdentity 생성.
 *
 * AuthService.issueTokens와 동일한 방식으로 JWT access/refresh를 발급한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public User provision(String provider, String providerUserId, String email, String displayName) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw ApiException.badRequest("OAuth provider did not return a user id");
        }

        return userIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserIdentity::getUser)
                .orElseGet(() -> linkOrCreate(provider, providerUserId, email, displayName));
    }

    private User linkOrCreate(String provider, String providerUserId, String email, String displayName) {
        User user = (email != null && !email.isBlank())
                ? userRepository.findByEmail(email).orElse(null)
                : null;

        if (user == null) {
            Role userRole = roleRepository.findByName(AuthService.DEFAULT_ROLE)
                    .orElseGet(() -> roleRepository.save(Role.of(AuthService.DEFAULT_ROLE)));
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);

            // OAuth 전용 계정: email이 없으면 placeholder 사용 (NOT NULL 제약)
            String effectiveEmail = (email != null && !email.isBlank())
                    ? email
                    : provider + ":" + providerUserId + "@oauth.local";

            user = User.builder()
                    .email(effectiveEmail)
                    .passwordHash(null)
                    .displayName(displayName)
                    .enabled(true)
                    .roles(roles)
                    .build();
            user = userRepository.save(user);
            log.info("Created new user from OAuth provider={} id={} email={}",
                    provider, user.getId(), user.getEmail());
        } else {
            log.info("Linking OAuth identity to existing user id={} provider={}",
                    user.getId(), provider);
        }

        UserIdentity identity = UserIdentity.builder()
                .provider(provider)
                .providerUserId(providerUserId)
                .user(user)
                .emailAtProvider(email)
                .build();
        userIdentityRepository.save(identity);

        return user;
    }

    @Transactional
    public TokenResponse issueTokens(User user) {
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
