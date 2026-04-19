package com.taejin.authservice.security.oauth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * OAuth2 인증 성공 후 {@link org.springframework.security.core.Authentication} 에
 * 실릴 principal. 이후 SuccessHandler에서 userId를 추출하여 JWT 발급에 사용한다.
 */
@Getter
@RequiredArgsConstructor
public class OAuth2UserPrincipal implements OAuth2User {

    private final Long userId;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
