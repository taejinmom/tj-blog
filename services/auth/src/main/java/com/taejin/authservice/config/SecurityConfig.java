package com.taejin.authservice.config;

import com.taejin.authservice.security.JwtAuthenticationFilter;
import com.taejin.authservice.security.RestAccessDeniedHandler;
import com.taejin.authservice.security.RestAuthenticationEntryPoint;
import com.taejin.authservice.security.oauth.CustomOAuth2UserService;
import com.taejin.authservice.security.oauth.LenientOAuth2AuthorizationRequestResolver;
import com.taejin.authservice.security.oauth.OAuth2AuthenticationFailureHandler;
import com.taejin.authservice.security.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**", "/h2-console/**", "/actuator/health",
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            .headers(h -> h.frameOptions(f -> f.sameOrigin())) // H2 console
            .oauth2Login(oauth -> oauth
                    // 커스텀 시작 경로: /api/auth/oauth2/authorize/{registrationId}
                    // LenientResolver: 알 수 없는 registrationId 는 예외 대신 null 반환 →
                    // 필터 체인 통과 후 컨트롤러가 없으므로 404(4xx)로 응답됨
                    .authorizationEndpoint(a -> a
                            .baseUri("/api/auth/oauth2/authorize")
                            .authorizationRequestResolver(new LenientOAuth2AuthorizationRequestResolver(
                                    clientRegistrationRepository,
                                    "/api/auth/oauth2/authorize")))
                    // 제공자 리다이렉트 콜백 경로: /api/auth/oauth2/callback/{registrationId}
                    .redirectionEndpoint(r -> r.baseUri("/api/auth/oauth2/callback/*"))
                    .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
