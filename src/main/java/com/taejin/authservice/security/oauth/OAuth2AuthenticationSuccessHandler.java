package com.taejin.authservice.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taejin.authservice.dto.TokenResponse;
import com.taejin.authservice.entity.User;
import com.taejin.authservice.repository.UserRepository;
import com.taejin.authservice.service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 시 자체 JWT(access/refresh)를 JSON 응답으로 반환한다.
 * SPA가 응답을 파싱해 토큰을 저장하는 흐름을 전제로 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthService oauthService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OAuth2UserPrincipal principal)) {
            log.warn("Unexpected principal type after OAuth2 login: {}",
                    authentication.getPrincipal().getClass());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected authentication principal");
            return;
        }

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "User disappeared after OAuth provisioning: " + principal.getUserId()));

        TokenResponse tokens = oauthService.issueTokens(user);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), tokens);
    }
}
