# CLAUDE.md

이 문서는 **auth-service 저장소에서 작업하는 Claude Code (및 이후 사람)** 를 위한
실무 가이드입니다. 프로젝트 소개/엔드포인트 카탈로그는 `README.md` 에 있습니다.
여기서는 **코드를 읽고 수정할 때 꼭 알아야 할 관례, 설계 의도, 함정** 을 정리합니다.

---

## 1. 스택 요약

- **런타임**: Java 17, Spring Boot 3.3.4, Gradle (Groovy DSL)
- **웹/보안**: Spring Web MVC, Spring Security, Spring OAuth2 Client
- **퍼시스턴스**: Spring Data JPA (Hibernate)
- **DB**: H2 인메모리(기본), PostgreSQL(향후 전환 대비 스텁 제공)
- **JWT**: jjwt 0.12.x, HS256, 자체 access/refresh 발급·검증
- **API 문서**: springdoc-openapi 2.6.x (Swagger UI)
- **검증**: Jakarta Bean Validation
- **유틸**: Lombok (`@RequiredArgsConstructor`, `@Slf4j`, `@Builder`, `@Getter/@Setter`)
- **테스트**: JUnit 5, Spring Security Test, MockMvc

---

## 2. 빌드 / 실행 / 테스트

```bash
./gradlew bootRun                  # 로컬 실행 (기본 포트 8080, 기본 프로파일 h2)
./gradlew test                     # 전체 테스트 (항상 application-test.yml 사용)
./gradlew test --tests AuthFlowIntegrationTest   # 단일 테스트
./gradlew clean build              # 전체 빌드
```

주요 URL:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:authdb`, user `sa`)
- 헬스체크: `http://localhost:8080/actuator/health`

---

## 3. DB 프로파일 (H2 / PostgreSQL 전환)

이 프로젝트는 **DB 프로파일 기반 전환** 구조입니다. 현재는 H2 인메모리만 사용하지만,
추후 PostgreSQL 로 옮길 수 있도록 설정 파일이 분리되어 있습니다.

### 파일 구성
```
src/main/resources/
├── application.yml             # 공통: 앱 이름, JWT, OAuth2, 로깅, profiles.active 기본값
├── application-h2.yml          # H2 인메모리 전용 datasource/jpa (기본 프로파일)
└── application-postgres.yml    # PostgreSQL 전환용 스텁 (현재 미사용)
src/test/resources/
└── application-test.yml        # 테스트 전용 (항상 H2, @ActiveProfiles("test") 로 강제)
```

### 선택 방법
`application.yml` 의 `spring.profiles.active` 는 환경변수 `DB_PROFILE` 로 제어합니다.
기본값은 `h2` 입니다.

```bash
./gradlew bootRun                      # → h2 (기본)
DB_PROFILE=postgres ./gradlew bootRun  # → postgres (스텁, 실제 DB 필요)
SPRING_PROFILES_ACTIVE=postgres ./gradlew bootRun  # 동일 효과, Spring 표준 방식
```

### H2 프로파일 (`application-h2.yml`)
- **메모리 전용** (`jdbc:h2:mem:authdb`) — 프로세스 종료 시 모든 데이터 소실. 영속 저장 용도로 절대 쓰지 말 것.
- `MODE=PostgreSQL` 로 SQL 방언을 postgres 에 최대한 맞춤 → 추후 postgres 전환 시 마찰 감소.
- `ddl-auto: update` — 엔티티 변경 시 자동으로 스키마 갱신.
- `/h2-console` 활성화, `SecurityConfig` 에서 `permitAll` + `frameOptions sameOrigin` 처리됨.

### PostgreSQL 프로파일 (`application-postgres.yml`) — 현재 스텁
- 파일 상단 TODO 체크리스트를 반드시 확인하고 활성화할 것. 이 단계까지 건너뛰면 운영에서 터집니다.
- 접속 정보는 `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` 환경변수로 주입.
- `ddl-auto` 는 스텁에서 편의상 `update` 로 두었지만, **실제 전환 시 `validate` 로 내리고 Flyway/Liquibase 도입 권장**.
- `build.gradle` 에 `org.postgresql:postgresql` 드라이버는 이미 `runtimeOnly` 로 포함되어 있으므로 별도 의존성 추가는 불필요.
- 로컬에서 띄울 땐 `docker run -d -p 5432:5432 -e POSTGRES_DB=authdb -e POSTGRES_USER=authservice -e POSTGRES_PASSWORD=authservice postgres:16` 정도가 가장 간단.

### DB 전환 시 체크리스트 (미래의 나에게)
- [ ] Flyway 또는 Liquibase 도입, JPA `ddl-auto` 는 `validate` 로 낮출 것
- [ ] `V1__init.sql` 에 현재 JPA 엔티티가 만드는 스키마를 dump 해서 기준 migration 으로 채택
- [ ] `refresh_tokens.token` 컬럼 길이(현재 `varchar(512)`) 확인, JWT 가 더 길어질 여지 있으면 `text` 로 조정
- [ ] 통합 테스트를 Testcontainers postgres 로 한 번 돌려서 H2 와의 방언 차이 검출
- [ ] `users.email` 등 UNIQUE 제약을 postgres 에서도 대소문자 정책이 맞는지 확인 (H2 PostgreSQL 모드도 기본은 case-sensitive)

---

## 4. 패키지 레이아웃 & 책임

```
com.taejin.authservice
├── AuthServiceApplication.java            # @SpringBootApplication 진입점
├── config
│   ├── SecurityConfig                     # FilterChain, oauth2Login, 경로별 권한
│   ├── JwtProperties                      # app.jwt.* 바인딩 (@ConfigurationProperties)
│   ├── OpenApiConfig                      # springdoc OpenAPI 메타데이터
│   └── RoleSeeder                         # ROLE_USER / ROLE_ADMIN 시드 (ApplicationRunner)
├── controller
│   ├── AuthController                     # /api/auth/{signup,login,refresh,logout}
│   ├── UserController                     # /api/users/me
│   └── AdminController                    # /api/admin/users
├── service
│   ├── AuthService                        # 로컬 signup/login/refresh/logout
│   └── OAuthService                       # 소셜 provision + 토큰 발급
├── repository                             # JPA Repository (User, Role, RefreshToken, UserIdentity)
├── entity                                 # JPA 엔티티 (아래 5장 참고)
├── dto                                    # Java record 기반 요청/응답 DTO
├── security
│   ├── JwtTokenProvider                   # JWT 생성/파싱/검증
│   ├── JwtAuthenticationFilter            # Authorization 헤더 → SecurityContext
│   ├── CustomUserDetailsService           # email → UserPrincipal
│   ├── UserPrincipal                      # UserDetails 구현체
│   ├── RestAuthenticationEntryPoint       # 401 JSON 응답
│   ├── RestAccessDeniedHandler            # 403 JSON 응답
│   └── oauth
│       ├── CustomOAuth2UserService        # 제공자 profile → OAuth2UserPrincipal
│       ├── OAuth2UserPrincipal            # OAuth2User + UserDetails
│       ├── OAuth2AttributeExtractor       # provider 분기 (google/github), GitHub name→login 폴백
│       ├── OAuth2AuthenticationSuccessHandler  # provision → JWT JSON 응답
│       ├── OAuth2AuthenticationFailureHandler  # 401 JSON
│       └── LenientOAuth2AuthorizationRequestResolver  # 미등록 provider → 예외 대신 null
└── exception
    ├── ApiException                       # code/status/message 가진 애플리케이션 예외
    └── GlobalExceptionHandler             # @RestControllerAdvice, 일관된 에러 JSON
```

---

## 5. 데이터 모델

### `users` (엔티티 `User`)
| 컬럼            | 타입/제약                      | 메모 |
|-----------------|--------------------------------|------|
| `id`            | PK, identity                   |      |
| `email`         | `varchar(255)` NOT NULL UNIQUE | 로컬/OAuth 공통 식별자 |
| `password_hash` | `varchar(255)` nullable        | BCrypt. OAuth 전용 계정은 null |
| `display_name`  | `varchar(100)` nullable        |      |
| `enabled`       | boolean NOT NULL               | 기본 true |
| `created_at`    | timestamp NOT NULL             | `@PrePersist` |
| `updated_at`    | timestamp NOT NULL             | `@PreUpdate` |

- `roles` : `user_roles` 조인 테이블을 통한 `ManyToMany`, `fetch = EAGER`.
- OAuth 전용 계정은 email 이 없을 수 있어 `{provider}:{providerUserId}@oauth.local` placeholder 사용.

### `roles` (엔티티 `Role`)
- `name` UNIQUE. 값은 항상 Spring Security 규약대로 **`ROLE_` 프리픽스** (`ROLE_USER`, `ROLE_ADMIN`).
- `RoleSeeder` 가 부팅 시 기본 2개를 보장.

### `refresh_tokens` (엔티티 `RefreshToken`)
| 컬럼         | 제약                          | 메모 |
|--------------|-------------------------------|------|
| `token`      | `varchar(512)` NOT NULL UNIQUE | JWT 문자열 그대로 저장 |
| `user_id`    | FK → users                    |      |
| `expires_at` | NOT NULL                      |      |
| `revoked`    | boolean NOT NULL              | 로그아웃/회전 시 true |
| `created_at` | NOT NULL                      | `@PrePersist` |

- `isActive()` = `!revoked && now < expiresAt`.
- `refresh` 엔드포인트가 JWT 문자열을 WHERE 절로 조회하므로 UNIQUE 위반이 나지 않도록 JWT 에 `jti`(UUID) 를 넣음 (6장 참고).

### `user_identities` (엔티티 `UserIdentity`)
- 외부 OAuth 계정과 내부 `User` 의 매핑.
- UNIQUE `(provider, provider_user_id)` — 중복 링크 방지.
- `email_at_provider` 는 참고용으로만 저장, `User.email` 과 꼭 같지 않아도 됨.
- 한 `User` 에 여러 `UserIdentity` 가능 (Google + GitHub 동시 연결).

---

## 6. JWT 규약 (반드시 유지해야 하는 불변식)

### 클레임 구조
| 클레임 | access | refresh | 비고 |
|--------|:------:|:-------:|------|
| `iss`  | ✓ | ✓ | `app.jwt.issuer` |
| `sub`  | ✓ | ✓ | userId (Long 을 String 으로) |
| `email`| ✓ |   | 편의용, 권위는 DB |
| `roles`| ✓ |   | `["ROLE_USER", ...]` 형태 |
| `typ`  | ✓ | ✓ | `"access"` / `"refresh"` — **타입 강제 검사 용도** |
| `jti`  |   | ✓ | UUID. refresh 동시발급 시 UNIQUE 충돌 방지 |
| `iat`, `exp` | ✓ | ✓ | 만료 기본 access 15분 / refresh 14일 |

### 반드시 지킬 것
1. **`typ` 검사를 빼지 말 것.** `AuthService.refresh` 는 `JwtTokenProvider.TYPE_REFRESH` 와 일치하는지 확인 후에만 DB 조회에 들어갑니다. access 를 refresh 로 악용하는 공격 벡터를 막기 위한 것.
2. **`JwtTokenProvider.createRefreshToken` 의 `jti`(UUID) 를 제거하지 말 것.** 동일 밀리초에 재발급되는 경우 토큰 문자열이 완전히 동일해져 `refresh_tokens.token UNIQUE` 제약 위반이 발생합니다. 실제로 그 이유로 추가된 장치.
3. **시크릿 길이**: `JwtTokenProvider.resolveSecretBytes` 는 Base64 디코드 시도 후 실패/길이 부족이면 raw UTF-8. **최소 32바이트** 미만이면 부팅 실패(`IllegalStateException`). 테스트/운영 시크릿 모두 이 조건을 만족해야 함.
4. **HS256 고정.** 알고리즘을 바꿀 경우 토큰 파싱/서명 경로 둘 다 수정해야 하며, jjwt 0.12.x 의 `Jwts.SIG.*` API 를 사용할 것.

### Refresh 회전 절차 (AuthService.refresh)
아래 4단계 순서를 바꾸지 말 것. 중간 단계를 건너뛰면 재사용 공격에 취약해집니다.

1. `tokenProvider.isValid(token)` + `typ == "refresh"` 확인
2. `refreshTokenRepository.findByToken(token)` + `isActive()` 확인
3. 기존 엔티티 `revoked = true` 로 저장
4. 새 access/refresh 발급 + `refresh_tokens` 에 새 행 insert

### 토큰 발급 경로가 두 곳
- 로컬: `AuthService.issueTokens(User)` *(private)*
- OAuth2: `OAuthService.issueTokens(User)` *(public, SuccessHandler 에서 호출)*

두 메서드는 **동일 로직을 중복 구현** 합니다. 토큰 발급 정책(클레임 추가, 만료 변경, 저장 방식 변경 등) 을 바꿀 때는 **양쪽 모두 반드시 수정** 하세요. 리팩터링 한다면 공통 헬퍼(`TokenIssuer`) 로 뽑는 것이 다음 단계에 좋습니다.

---

## 7. 인증 필터 체인과 권한 규칙

### `SecurityConfig` 핵심
- `sessionCreationPolicy: STATELESS` — HTTP 세션 없음, 모든 요청은 JWT 또는 익명.
- `csrf`, `formLogin`, `httpBasic` 비활성화.
- `exceptionHandling` → 401 은 `RestAuthenticationEntryPoint`, 403 은 `RestAccessDeniedHandler` (둘 다 JSON).
- `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`.

### 경로 규칙
| 패턴 | 권한 |
|------|------|
| `/api/auth/**`, `/h2-console/**`, `/actuator/health`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` | `permitAll` |
| `/api/admin/**` | `hasRole("ADMIN")` *(자동으로 `ROLE_` 프리픽스)* |
| 그 외 | `authenticated` |

### OAuth2 커스터마이즈 포인트
- `authorizationEndpoint.baseUri`: `/api/auth/oauth2/authorize` *(Spring 기본 `/oauth2/authorization` 아님)*
- `redirectionEndpoint.baseUri`: `/api/auth/oauth2/callback/*` — Google/GitHub OAuth 앱 콘솔에 **정확히 이 URL** 을 등록해야 함. 불일치 시 `redirect_uri_mismatch`.
- `LenientOAuth2AuthorizationRequestResolver` : 등록되지 않은 provider 에 대해 `null` 반환 → 필터 체인 통과 후 404 로 귀결. 이 동작을 기대하는 테스트가 있으므로 일반 `DefaultOAuth2AuthorizationRequestResolver` 로 교체 시 테스트가 깨집니다.
- `userInfoEndpoint.userService`: `CustomOAuth2UserService` → 제공자 profile 을 `OAuth2UserPrincipal` 로 변환.
- `successHandler` 가 `OAuthService.provision` + `issueTokens` 호출 → JSON 응답 (`{accessToken, refreshToken, tokenType:"Bearer"}`).

### 이중 방어 (Belt & Suspenders)
`@EnableMethodSecurity` 활성화 + `@PreAuthorize` 를 컨트롤러/서비스에 병행 사용할 수 있는 구조. 관리자 엔드포인트는 **HTTP 매처와 `@PreAuthorize` 양쪽** 에 규칙을 넣는 기존 패턴을 유지하세요.

---

## 8. OAuth2 프로비저닝 규칙 (`OAuthService.provision`)

우선순위 (이 순서를 절대 바꾸지 말 것):

1. **기존 identity 매칭**: `(provider, providerUserId)` 로 `UserIdentity` 조회 → 찾으면 연결된 `User` 그대로 반환 (멱등).
2. **이메일 자동 링크**: 제공자가 준 email 과 동일한 email 의 로컬 `User` 가 이미 있으면 → 해당 User 에 `UserIdentity` 만 추가. **로컬 비밀번호/역할을 절대 덮어쓰지 말 것.**
3. **신규 생성**: 새 `User` (`passwordHash=null`, `ROLE_USER`) + `UserIdentity`. 제공자가 email 을 주지 않으면 `{provider}:{providerUserId}@oauth.local` placeholder 사용 (User.email 이 NOT NULL + UNIQUE).

`OAuth2AttributeExtractor` 는 provider 별 profile 파싱:
- **Google**: `sub`, `email`, `name`
- **GitHub**: `id`, `email`, `name` → `name` 이 null 이면 `login` 으로 폴백
- **미지원 provider**: `ApiException` (단위 테스트 존재)

새 provider 를 추가할 때는:
1. `application.yml` 의 `spring.security.oauth2.client.registration.*` 추가
2. `OAuth2AttributeExtractor` 에 분기 추가
3. `OAuth2AttributeExtractorTest` 에 케이스 추가
4. `application-test.yml` 에 더미 client-id/secret 추가 (미설정 시 테스트에서 registration 로딩 실패 가능)

---

## 9. 코딩 컨벤션

### 네이밍 & 구조
- **패키지 구조는 레이어 기반** (`controller/`, `service/`, `repository/`, `entity/`, `dto/`). 현재는 단일 도메인이라 이대로 충분.
- **DTO 는 Java `record`** 우선 (`SignupRequest`, `LoginRequest`, `TokenResponse`, `UserResponse` 등). 가변 필드가 꼭 필요한 경우에만 class.
- **엔티티는 Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`** 패턴. Setter 는 JPA/테스트 편의상 열어두고 있으며, 외부에서 함부로 호출하지 말 것.
- **서비스는 `@Service @RequiredArgsConstructor`** + `final` 필드 생성자 주입. 필드 주입(`@Autowired` 필드) 금지.

### 트랜잭션
- **상태 변경 메서드에는 `@Transactional`** (예: `signup`, `login`, `refresh`, `logout`, `provision`).
- 읽기 전용이 분명하면 `@Transactional(readOnly = true)` 사용.
- `open-in-view: false` 로 두었으므로 (application.yml) 컨트롤러 레이어에서 Lazy 컬렉션 접근하지 말 것. 필요한 데이터는 서비스/DTO 에서 확정해서 내려보낼 것.

### 예외 처리
- **`ApiException` 팩토리만 사용**: `ApiException.badRequest(...)`, `unauthorized(...)`, `forbidden(...)`, `notFound(...)`, `conflict(...)` 등.
- 비즈니스 레이어에서 `RuntimeException`, `IllegalArgumentException`, `IllegalStateException` 을 직접 던지지 말 것. `GlobalExceptionHandler` 가 일관된 JSON 응답을 내주는 경로는 `ApiException` 뿐.
- Spring Security 예외(`BadCredentialsException` 등)는 서비스 내부에서 잡아 `ApiException.unauthorized(...)` 로 변환 (현재 `AuthService.login` 참고).

### 로깅
- `@Slf4j`, 로그 레벨 가이드:
  - `error` — 복구 불가/조사 필요
  - `warn` — 예상 가능한 실패(잘못된 토큰 등)지만 기록 가치 있음
  - `info` — 상태 전이(회원가입, OAuth 신규 생성, 링크)
  - `debug` — 디버깅 보조 (토큰 파싱 실패 등)
- **민감정보(비밀번호, 토큰 전체 문자열)는 절대 로깅하지 말 것.** userId, email 까지는 info 허용.

### 보안 습관
- 평문 비밀번호 비교/저장 금지. 항상 `PasswordEncoder` 경유.
- JWT 시크릿, OAuth client secret 을 코드/리포에 하드코딩하지 말 것. 환경변수 또는 시크릿 매니저.
- 예외 메시지에 사용자 존재 여부를 흘리지 말 것. 로그인 실패는 일관되게 `"Invalid email or password"`.

---

## 10. 테스트 작성 가이드

### 테스트 구성
- `integration/` — `@SpringBootTest` + `MockMvc` 로 실제 엔드포인트-서비스-DB(H2) 왕복 검증.
- `security/oauth/` — 순수 단위 테스트 (프레임워크 없이 `OAuth2AttributeExtractor` 같은 유틸 검증).
- 모든 통합 테스트는 `@ActiveProfiles("test")` → `application-test.yml` 로드.
  - 메인의 `DB_PROFILE` 은 무시됨 (test 가 우선순위 높음). H2 create-drop 으로 격리.

### 새 엔드포인트 추가 시
1. `AuthFlowIntegrationTest` 류에서 **성공 케이스 1 + 실패 케이스 ≥1** 추가.
2. 권한이 필요한 엔드포인트면 `RbacIntegrationTest` 스타일로 `ROLE_USER`/`ROLE_ADMIN` 분리 검증.
3. 토큰 관련 변경이면 **잘못된 `typ`, 변조, 만료** 케이스까지 챙길 것 — 기존 테스트의 패턴을 따라가면 무료로 얻어집니다.

### 데이터 격리 주의
- H2 는 `create-drop` 이라 클래스 단위로는 초기화되지만, 테스트 메서드 간에는 DB 가 공유됩니다.
- 이메일 충돌을 피하기 위해 **테스트마다 유니크한 email** (e.g. `"user-" + UUID.randomUUID() + "@ex.com"`) 을 쓰는 기존 패턴을 따르세요. `@Transactional` 로 롤백하는 방식은 `@SpringBootTest` + MockMvc 조합에서 트랜잭션 경계가 꼬이는 경우가 있어 권장하지 않습니다.

### MockMvc 스타일
- 요청 바디: `objectMapper.writeValueAsString(record)` 로 JSON 직렬화.
- 응답 파싱: `mockMvc.perform(...).andReturn().getResponse().getContentAsString()` → `objectMapper.readValue(..., TokenResponse.class)`.
- JWT 검사가 필요하면 `JwtTokenProvider` 를 스프링 컨텍스트에서 `@Autowired` 로 꺼내 쓰세요 — 테스트 프로파일에도 동일 빈이 있습니다.

---

## 11. 환경변수 레퍼런스

| 이름 | 기본값 | 설명 |
|------|--------|------|
| `DB_PROFILE` | `h2` | `h2` 또는 `postgres` (향후) |
| `SPRING_PROFILES_ACTIVE` | (없음) | Spring 표준. 설정 시 `DB_PROFILE` 보다 우선 |
| `JWT_SECRET` | 개발용 플레이스홀더 | 최소 32바이트. Base64 시도 후 실패하면 raw UTF-8 |
| `GOOGLE_CLIENT_ID` | - | 비어 있으면 Google registration 미로드 → 소셜 로그인 비활성 |
| `GOOGLE_CLIENT_SECRET` | - | 위와 동일 |
| `GITHUB_CLIENT_ID` | - | 위와 동일 |
| `GITHUB_CLIENT_SECRET` | - | 위와 동일 |
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/authdb` | postgres 프로파일용 |
| `POSTGRES_USER` | `authservice` | postgres 프로파일용 |
| `POSTGRES_PASSWORD` | `authservice` | postgres 프로파일용 |

운영에서는 `JWT_SECRET` 을 반드시 추측 불가능한 고엔트로피 값으로 교체할 것.

---

## 12. 자주 수정하는 작업별 체크리스트

### "토큰 정책을 바꿨다" (만료, 클레임, 알고리즘)
- [ ] `JwtTokenProvider` 의 create/parse 양쪽에 반영했는가?
- [ ] `AuthService.issueTokens` 와 `OAuthService.issueTokens` 양쪽에 반영했는가?
- [ ] `AuthFlowIntegrationTest` 의 토큰 검증 부분을 업데이트했는가?
- [ ] `typ` 검사 로직을 깨뜨리지 않았는가?
- [ ] refresh 토큰 길이가 늘어났다면 `refresh_tokens.token` 컬럼 길이(512)가 충분한가?

### "새 엔드포인트 추가"
- [ ] `SecurityConfig.authorizeHttpRequests` 에 경로 규칙을 넣었는가?
- [ ] 권한이 필요하면 `@PreAuthorize` 도 같이 넣었는가? (이중 방어)
- [ ] DTO 는 `record` 인가? `@Valid` + Bean Validation 애너테이션을 붙였는가?
- [ ] 성공/실패 테스트 모두 있는가?

### "새 OAuth provider 추가"
- [ ] `application.yml` registration 추가
- [ ] `application-test.yml` 에 더미 client-id/secret 추가
- [ ] `OAuth2AttributeExtractor` 분기 추가
- [ ] `OAuth2AttributeExtractorTest` 케이스 추가
- [ ] Google/GitHub OAuth 앱 콘솔에 `/api/auth/oauth2/callback/{provider}` 등록 안내를 README 에 반영

### "엔티티 스키마 변경"
- [ ] H2 는 `ddl-auto: update` 라 대부분 자동이지만, **컬럼 타입 변경/제약 삭제** 는 실패할 수 있음 — 로컬 재시작 시 인메모리라 문제 없음
- [ ] postgres 전환 이후라면 Flyway migration 파일 추가 필수
- [ ] 기존 레포지토리 쿼리 메서드 네이밍이 여전히 유효한지 확인 (`findByEmail` 등)

### "에러 응답 포맷을 바꿨다"
- [ ] `GlobalExceptionHandler` 와 `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler` **세 곳 모두** 일관되게 수정했는가? (401/403 은 필터 단계에서 이 두 핸들러가 담당, 그 외는 Advice)

### 공통: 커밋 전
- [ ] `./gradlew test` 초록
- [ ] 새 환경변수/설정이 있다면 `README.md` 와 이 `CLAUDE.md` 의 11장에 반영
- [ ] 커밋 메시지는 한국어/영어 일관되게 (`README.md` 스타일 참고)

---

## 13. 알려진 TODO / 기술 부채

- **Flyway/Liquibase 미도입** — 현재는 JPA `ddl-auto: update` 에 의존. postgres 전환의 전제조건.
- **토큰 발급 로직 중복** — `AuthService.issueTokens` vs `OAuthService.issueTokens`. 공통 `TokenIssuer` 로 추출 대상.
- **Refresh 토큰 GC 없음** — 만료된 `refresh_tokens` 행이 쌓임. 스케줄러 또는 DB partial index 로 정리 필요 (postgres 전환 후).
- **Rate limiting 없음** — `/api/auth/login` 브루트포스 방어 없음. 프로덕션 전 필수.
- **Audit 로그 없음** — 로그인/로그아웃/권한 부여 이력 저장 없음. 요구사항 생기면 별도 테이블/이벤트.
- **이메일 검증 없음** — 가입 직후 `enabled=true`. 이메일 소유권 검증 플로우 미구현.
