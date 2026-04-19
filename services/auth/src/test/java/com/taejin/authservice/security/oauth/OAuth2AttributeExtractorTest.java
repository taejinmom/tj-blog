package com.taejin.authservice.security.oauth;

import com.taejin.authservice.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 제공자별 속성 추출 분기 단위 테스트.
 */
class OAuth2AttributeExtractorTest {

    @Test
    @DisplayName("Google: sub / email / name 추출")
    void extractGoogle() {
        Map<String, Object> attrs = Map.of(
                "sub", "112233445566",
                "email", "user@gmail.com",
                "name", "Google User"
        );
        OAuth2AttributeExtractor.Extracted result =
                OAuth2AttributeExtractor.extract("google", attrs);

        assertThat(result.providerUserId()).isEqualTo("112233445566");
        assertThat(result.email()).isEqualTo("user@gmail.com");
        assertThat(result.displayName()).isEqualTo("Google User");
    }

    @Test
    @DisplayName("GitHub: id / email / name 추출, name 있으면 name 사용")
    void extractGithubWithName() {
        Map<String, Object> attrs = Map.of(
                "id", 987654,
                "email", "dev@github.example",
                "name", "Dev Name",
                "login", "devlogin"
        );
        OAuth2AttributeExtractor.Extracted result =
                OAuth2AttributeExtractor.extract("github", attrs);

        assertThat(result.providerUserId()).isEqualTo("987654");
        assertThat(result.email()).isEqualTo("dev@github.example");
        assertThat(result.displayName()).isEqualTo("Dev Name");
    }

    @Test
    @DisplayName("GitHub: name 이 null 이면 login 으로 폴백")
    void extractGithubFallsBackToLogin() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", 42);
        attrs.put("email", null);
        attrs.put("name", null);
        attrs.put("login", "octocat");

        OAuth2AttributeExtractor.Extracted result =
                OAuth2AttributeExtractor.extract("github", attrs);

        assertThat(result.providerUserId()).isEqualTo("42");
        assertThat(result.email()).isNull();
        assertThat(result.displayName()).isEqualTo("octocat");
    }

    @Test
    @DisplayName("지원하지 않는 제공자는 ApiException 발생")
    void extractUnsupportedProviderThrows() {
        Map<String, Object> attrs = Map.of("sub", "x");
        assertThatThrownBy(() -> OAuth2AttributeExtractor.extract("facebook", attrs))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("facebook");
    }
}
