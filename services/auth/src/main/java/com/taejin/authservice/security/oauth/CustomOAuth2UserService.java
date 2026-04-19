package com.taejin.authservice.security.oauth;

import com.taejin.authservice.entity.Role;
import com.taejin.authservice.entity.User;
import com.taejin.authservice.service.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 제공자의 userinfo 응답을 가져와서 {@link OAuthService}에 프로비저닝을 위임하고,
 * 후속 SuccessHandler에서 사용할 수 있도록 내부 userId를 담은 principal을 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuthService oauthService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User delegate = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        try {
            OAuth2AttributeExtractor.Extracted ex =
                    OAuth2AttributeExtractor.extract(registrationId, delegate.getAttributes());

            User user = oauthService.provision(
                    registrationId, ex.providerUserId(), ex.email(), ex.displayName());

            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(Role::getName)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return new OAuth2UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    authorities,
                    delegate.getAttributes()
            );
        } catch (RuntimeException e) {
            log.error("OAuth2 user provisioning failed for provider={}", registrationId, e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("provisioning_failed", e.getMessage(), null), e);
        }
    }
}
