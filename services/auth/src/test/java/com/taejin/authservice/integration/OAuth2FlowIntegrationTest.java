package com.taejin.authservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taejin.authservice.dto.SignupRequest;
import com.taejin.authservice.dto.TokenResponse;
import com.taejin.authservice.entity.User;
import com.taejin.authservice.entity.UserIdentity;
import com.taejin.authservice.repository.UserIdentityRepository;
import com.taejin.authservice.repository.UserRepository;
import com.taejin.authservice.security.JwtTokenProvider;
import com.taejin.authservice.service.OAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OAuth2 소셜 로그인 통합 테스트.
 *
 * 실제 Google/GitHub 호출은 외부 의존성이 크므로, 여기서는
 *  1) Spring Security 가 제공하는 authorize 엔드포인트가 제공자로 302 리다이렉트하는지
 *  2) OAuthService 의 provision/issueTokens 플로우(신규/링크)가 DB 상태를 기대대로 변경하는지
 *  를 검증한다. 실제 토큰 교환까지의 end-to-end 는 수동 QA 나 e2e 환경에 남긴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2FlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OAuthService oauthService;
    @Autowired UserRepository userRepository;
    @Autowired UserIdentityRepository userIdentityRepository;
    @Autowired JwtTokenProvider tokenProvider;

    @Test
    @DisplayName("/api/auth/oauth2/authorize/google 은 Google 인증 페이지로 302 리다이렉트")
    void authorizeGoogleRedirects() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/authorize/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }

    @Test
    @DisplayName("/api/auth/oauth2/authorize/github 은 GitHub 인증 페이지로 302 리다이렉트")
    void authorizeGithubRedirects() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/authorize/github"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("github.com/login/oauth/authorize")));
    }

    @Test
    @DisplayName("/api/auth/oauth2/authorize/unknown 은 알 수 없는 제공자이므로 실패")
    void authorizeUnknownProvider() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/authorize/unknown"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("OAuthService.provision: 신규 제공자 사용자 → 새 User + UserIdentity 생성")
    void provisionCreatesNewUser() {
        String email = "new-oauth@example.com";
        User user = oauthService.provision("google", "google-sub-1", email, "OAuth New");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getRoles()).extracting("name").contains("ROLE_USER");

        Optional<UserIdentity> identity =
                userIdentityRepository.findByProviderAndProviderUserId("google", "google-sub-1");
        assertThat(identity).isPresent();
        assertThat(identity.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(identity.get().getEmailAtProvider()).isEqualTo(email);
    }

    @Test
    @DisplayName("OAuthService.provision: 이메일이 기존 로컬 User 와 같으면 자동 연결")
    void provisionLinksExistingUserByEmail() throws Exception {
        String email = "local-first@example.com";

        // 먼저 로컬 가입
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(email, "Password1234", "Local First"))))
                .andExpect(status().isOk());

        User existing = userRepository.findByEmail(email).orElseThrow();

        // 이제 동일 이메일로 OAuth 프로비저닝
        User provisioned = oauthService.provision("github", "gh-42", email, "Local First");
        assertThat(provisioned.getId()).isEqualTo(existing.getId());

        // 로컬 가입자의 passwordHash는 보존되어야 한다
        User reloaded = userRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getPasswordHash()).isNotNull();

        // UserIdentity 가 연결되어 있어야 한다
        assertThat(userIdentityRepository.findByProviderAndProviderUserId("github", "gh-42"))
                .isPresent();
    }

    @Test
    @DisplayName("OAuthService.provision: 같은 (provider, providerUserId) 재호출 시 동일 User 재사용")
    void provisionIsIdempotentPerIdentity() {
        User first = oauthService.provision("google", "sub-stable", "stable@example.com", "Stable");
        User second = oauthService.provision("google", "sub-stable", "stable@example.com", "Stable");
        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("OAuthService.issueTokens: 발급된 access/refresh 토큰은 올바른 typ 를 가진다")
    void issueTokensHaveCorrectType() {
        User user = oauthService.provision("google", "token-sub", "token@example.com", "Token User");
        TokenResponse tokens = oauthService.issueTokens(user);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokenProvider.getType(tokens.accessToken()))
                .isEqualTo(JwtTokenProvider.TYPE_ACCESS);
        assertThat(tokenProvider.getType(tokens.refreshToken()))
                .isEqualTo(JwtTokenProvider.TYPE_REFRESH);
        List<String> roles = tokenProvider.getRoles(tokens.accessToken());
        assertThat(roles).contains("ROLE_USER");
    }

    @Test
    @DisplayName("OAuthService.provision: provider가 email을 주지 않으면 placeholder 이메일로 생성")
    void provisionHandlesMissingEmail() {
        User user = oauthService.provision("github", "no-email-user", null, "No Email");
        assertThat(user.getEmail()).contains("@oauth.local");
    }
}
