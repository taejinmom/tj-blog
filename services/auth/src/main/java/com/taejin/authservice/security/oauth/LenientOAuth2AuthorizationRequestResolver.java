package com.taejin.authservice.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * {@link DefaultOAuth2AuthorizationRequestResolver}를 래핑해서
 * 알 수 없는 registrationId 로 요청이 들어올 때 예외를 던지는 대신 null 을 반환한다.
 *
 * null 을 반환하면 {@code OAuth2AuthorizationRequestRedirectFilter}는 필터 체인을 계속
 * 진행시키고, 매핑된 컨트롤러가 없으므로 최종적으로 4xx(Not Found) 응답이 반환된다.
 * 기본 동작은 IllegalArgumentException 을 던져 500 이 되는데, 알 수 없는 provider 를
 * 요청한 것은 클라이언트 오류에 해당하므로 4xx 로 노출되는 편이 자연스럽다.
 */
public class LenientOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public LenientOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo,
                                                     String authorizationRequestBaseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        try {
            return delegate.resolve(request);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        try {
            return delegate.resolve(request, clientRegistrationId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
