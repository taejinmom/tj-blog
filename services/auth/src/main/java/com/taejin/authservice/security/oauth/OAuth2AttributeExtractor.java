package com.taejin.authservice.security.oauth;

import com.taejin.authservice.exception.ApiException;

import java.util.Map;

/**
 * 제공자별 속성 맵에서 공통 필드(subject, email, displayName)를 추출한다.
 *
 * - Google: sub, email, name
 * - GitHub: id, email(null 가능), name(또는 login)
 */
public final class OAuth2AttributeExtractor {

    public static final String PROVIDER_GOOGLE = "google";
    public static final String PROVIDER_GITHUB = "github";

    private OAuth2AttributeExtractor() {}

    public record Extracted(String providerUserId, String email, String displayName) {}

    public static Extracted extract(String registrationId, Map<String, Object> attrs) {
        return switch (registrationId) {
            case PROVIDER_GOOGLE -> new Extracted(
                    asString(attrs.get("sub")),
                    asString(attrs.get("email")),
                    asString(attrs.get("name"))
            );
            case PROVIDER_GITHUB -> new Extracted(
                    asString(attrs.get("id")),
                    asString(attrs.get("email")),
                    // name이 null이면 login으로 폴백 (getOrDefault는 key 부재 시에만 동작하므로 명시적으로 처리)
                    asString(attrs.get("name") != null ? attrs.get("name") : attrs.get("login"))
            );
            default -> throw ApiException.badRequest("Unsupported OAuth provider: " + registrationId);
        };
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }
}
