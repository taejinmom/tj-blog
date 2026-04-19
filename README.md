# auth-service

Spring Boot 3.x + Java 17 기반 인증 서비스. 로컬 회원가입/로그인, JWT 액세스/리프레시 토큰, RBAC, OAuth2 소셜 로그인을 제공합니다.

## Stack
- Spring Boot 3.3.x, Java 17, Gradle
- Spring Security, Spring Data JPA
- jjwt 0.12.x (HS256)
- springdoc-openapi 2.6.x (Swagger UI)
- H2 인메모리 (기본) / PostgreSQL (프로파일 스텁)
- Lombok, Bean Validation
- JUnit5 + MockMvc (통합 테스트)

## Getting Started

### 사전 준비물
| 항목 | 버전 | 비고 |
|------|------|------|
| **JDK** | 17 이상 | `java -version` 으로 확인. Temurin 17 권장 |
| **Git** | 최신 | |
| **Gradle** | — | `gradlew` 가 자동 다운로드, 별도 설치 불필요 |
| **DB** | — | H2 인메모리 내장, 외부 DB 불필요 |

#### JDK 설치 예시
- **Windows**: `winget install EclipseAdoptium.Temurin.17.JDK`
- **macOS**: `brew install --cask temurin@17`
- **Ubuntu/WSL**: `sudo apt install -y openjdk-17-jdk`

설치 후 새 터미널에서 `java -version` 이 `17.x` 로 출력되는지 확인하세요.

### 클론 & 실행
```bash
git clone https://github.com/taejinmom/auth-service.git
cd auth-service

# Linux / macOS
./gradlew bootRun

# Windows (PowerShell / cmd)
.\gradlew.bat bootRun
```
처음 실행 시 Gradle 배포판과 의존성 다운로드로 몇 분 걸릴 수 있습니다.
콘솔에 `Started AuthServiceApplication in X seconds` 가 찍히면 성공입니다.

### 동작 확인
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- 헬스체크: <http://localhost:8080/actuator/health>
- H2 콘솔: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:authdb`, user `sa`, password 비움)

간단한 회원가입 → 로그인 흐름:
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass1234","displayName":"tester"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass1234"}'
```

### 테스트 실행
```bash
./gradlew test
```
테스트는 외부 의존성 없이 H2 인메모리로 완결됩니다.

### 서비스 사용법
브라우저·Swagger·H2 콘솔·curl 로 직접 체험해보는 종단 간 사용 가이드는 **[USAGE.md](./USAGE.md)** 를 참고하세요. 회원가입 → 로그인 → 보호된 API 호출 → 관리자 권한 부여 → 토큰 rotation 시나리오를 단계별로 안내합니다.

### 트러블슈팅
- **`JAVA_HOME is not set`** → JDK 설치 후 새 터미널을 열거나, `JAVA_HOME` 환경변수를 JDK 경로로 지정.
- **포트 8080 충돌** → `./gradlew bootRun --args='--server.port=8081'` 처럼 포트를 오버라이드.
- **재시작 시 데이터가 사라짐** → H2 인메모리라 정상. 영속 저장이 필요하면 아래 [DB 프로파일](#db-프로파일-h2--postgresql) 참고.
- **OAuth `redirect_uri_mismatch`** → Google/GitHub 콘솔에 등록한 콜백 URL 이 `http://localhost:8080/api/auth/oauth2/callback/{google|github}` 과 정확히 일치하는지 확인.

## DB 프로파일 (H2 / PostgreSQL)

DB 설정은 Spring 프로파일로 분리되어 있습니다.

| 프로파일 | 파일 | 상태 | 용도 |
|----------|------|------|------|
| `h2` (기본) | `application-h2.yml` | ✅ 사용 중 | 인메모리 개발/데모 |
| `postgres` | `application-postgres.yml` | 🚧 스텁 | 추후 전환 대비 |
| `test` | `src/test/resources/application-test.yml` | ✅ 사용 중 | 통합 테스트 (H2 create-drop) |

기본값은 `h2` 이며, 환경변수로 오버라이드할 수 있습니다.
```bash
./gradlew bootRun                         # h2 (기본)
DB_PROFILE=postgres ./gradlew bootRun     # postgres (스텁, 실제 DB 필요)
SPRING_PROFILES_ACTIVE=postgres ./gradlew bootRun   # Spring 표준 방식 (우선순위 높음)
```

- **H2** 는 **메모리 전용** (`jdbc:h2:mem:authdb`) 입니다. 프로세스 종료 시 데이터가 전부 사라집니다.
  `MODE=PostgreSQL` 로 두어 SQL 방언을 postgres 와 유사하게 맞춰 두었습니다.
- **PostgreSQL** 프로파일은 현재 **스텁** 입니다. 실제 전환 전에 `application-postgres.yml` 상단의 TODO (Flyway 도입, `ddl-auto: validate` 전환 등) 를 해결해야 합니다. 드라이버는 `build.gradle` 에 이미 포함되어 있습니다.

## Package structure
```
com.taejin.authservice
├── config         # Spring/Security/OpenAPI 설정 (SecurityConfig, OpenApiConfig, JwtProperties, RoleSeeder)
├── controller     # REST 컨트롤러 (AuthController, UserController, AdminController)
├── service        # 비즈니스 로직 (AuthService, OAuthService)
├── repository     # JPA 리포지토리 (User, Role, RefreshToken, UserIdentity)
├── entity         # JPA 엔티티 (User, Role, RefreshToken, UserIdentity)
├── dto            # 요청/응답 DTO
├── security       # JwtTokenProvider, JwtAuthenticationFilter, UserDetails 서비스, 엔트리포인트/핸들러
│   └── oauth      # CustomOAuth2UserService, OAuth2UserPrincipal, 성공/실패 핸들러, 속성 추출기
└── exception      # 전역 예외 처리 (ApiException, GlobalExceptionHandler)
```

## Run
```bash
./gradlew bootRun
```
기본 포트: `8080`

주요 URL:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:authdb`)

## Test
```bash
./gradlew test
```
통합 테스트는 `test` 프로파일(H2 인메모리)로 실행되며, 전체 인증 플로우와 RBAC 권한 분리를 검증합니다.

주요 테스트:
- `AuthFlowIntegrationTest` — signup → login → `/api/users/me` → refresh → logout 전체 플로우, 실패 케이스(잘못된 비밀번호, 잘못된 typ, 변조 토큰, 중복 가입)
- `RbacIntegrationTest` — `ROLE_USER`/`ROLE_ADMIN` 권한 분리, `/api/admin/users` 접근 제어
- `OAuth2FlowIntegrationTest` — `/api/auth/oauth2/authorize/{provider}` 리다이렉트, `OAuthService.provision`의 신규/링크/멱등성 검증, 토큰 typ 검증
- `OAuth2AttributeExtractorTest` — Google/GitHub 속성 추출 분기 단위 테스트 (GitHub `name` null 시 `login` 폴백, 미지원 제공자 예외)
- `OpenApiSmokeTest` — `/v3/api-docs` 와 `/swagger-ui/index.html` 미인증 접근 가능 확인

## Endpoints

### 로컬 인증 (`/api/auth/**`)
| Method | Path                 | Auth | 설명                                              |
|--------|----------------------|------|---------------------------------------------------|
| POST   | `/api/auth/signup`   | -    | 이메일/비밀번호로 회원가입. `ROLE_USER` 자동 부여 |
| POST   | `/api/auth/login`    | -    | 로그인. 성공 시 access/refresh 토큰 발급          |
| POST   | `/api/auth/refresh`  | -    | refresh 토큰으로 토큰 재발급(rotation)            |
| POST   | `/api/auth/logout`   | -    | refresh 토큰 폐기                                 |

### 사용자 (`/api/users/**`)
| Method | Path             | Auth           | 설명                     |
|--------|------------------|----------------|--------------------------|
| GET    | `/api/users/me`  | Bearer(JWT)    | 현재 로그인 사용자 조회  |

### 관리자 (`/api/admin/**`)
| Method | Path                | Auth                  | 설명                 |
|--------|---------------------|-----------------------|----------------------|
| GET    | `/api/admin/users`  | Bearer + `ROLE_ADMIN` | 전체 사용자 목록 조회 |

### 헬스 / 문서
| Method | Path                 | Auth | 설명           |
|--------|----------------------|------|----------------|
| GET    | `/actuator/health`   | -    | 헬스체크       |
| GET    | `/swagger-ui.html`   | -    | Swagger UI     |
| GET    | `/v3/api-docs`       | -    | OpenAPI 스펙   |

### OAuth2 소셜 로그인 (`/api/auth/oauth2/**`)
Google / GitHub 소셜 로그인을 지원합니다. SPA 친화적으로 성공 시 JSON 으로 자체 JWT access/refresh 토큰을 반환합니다.

| Method | Path                                                  | Auth | 설명                                                 |
|--------|-------------------------------------------------------|------|------------------------------------------------------|
| GET    | `/api/auth/oauth2/authorize/google`                   | -    | Google 인증 페이지로 302 리다이렉트                  |
| GET    | `/api/auth/oauth2/authorize/github`                   | -    | GitHub 인증 페이지로 302 리다이렉트                  |
| GET    | `/api/auth/oauth2/callback/{provider}`                | -    | OAuth2 콜백. 성공 시 JSON `{accessToken, refreshToken, tokenType}` 반환, 실패 시 401 |

프로비저닝 규칙 (`OAuthService.provision`):
1. 기존에 동일한 `(provider, providerUserId)` 조합의 `UserIdentity` 가 있으면 연결된 User 를 그대로 사용
2. 없으면, 제공자가 준 이메일이 기존 로컬 User 의 이메일과 같으면 해당 User 에 `UserIdentity` 만 추가 (비밀번호/역할 보존)
3. 둘 다 아니면 새 User(비밀번호 없음, `ROLE_USER`) + `UserIdentity` 생성. 제공자가 이메일을 주지 않으면 `{provider}:{providerUserId}@oauth.local` 형태의 placeholder 사용

환경변수(`GOOGLE_CLIENT_ID/SECRET`, `GITHUB_CLIENT_ID/SECRET`)가 비어 있으면 해당 제공자 registration 이 로드되지 않아 소셜 로그인은 비활성화됩니다. 테스트 프로파일에서는 더미 client-id/secret 을 주입합니다.

#### 로컬에서 OAuth 테스트하기

1. **Google Cloud Console** → APIs & Services → Credentials → "Create Credentials" → "OAuth client ID"
   - Application type: **Web application**
   - Authorized redirect URI: `http://localhost:8080/api/auth/oauth2/callback/google`
   - 생성 후 Client ID / Client Secret 을 환경변수 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 에 설정
2. **GitHub** → Settings → Developer settings → OAuth Apps → "New OAuth App"
   - Homepage URL: `http://localhost:8080`
   - Authorization callback URL: `http://localhost:8080/api/auth/oauth2/callback/github`
   - 생성 후 Client ID / Client Secret 을 환경변수 `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` 에 설정
3. 애플리케이션 실행 후 브라우저에서 `http://localhost:8080/api/auth/oauth2/authorize/google` 또는 `/github` 로 접근
4. 제공자 인증 완료 시 콜백이 자체 JWT 를 JSON 으로 반환 (SPA 가 이 응답을 파싱해 access/refresh 토큰을 저장하는 플로우를 전제)

`redirect_uri_mismatch` 에러가 나면 콘솔에 등록한 URI 와 SecurityConfig 의 `redirectionEndpoint` (`/api/auth/oauth2/callback/*`) 가 정확히 일치하는지 확인하세요.

## Authentication
1. `POST /api/auth/signup` 으로 계정 생성
2. `POST /api/auth/login` 으로 access/refresh 토큰 획득
3. 보호된 API 호출 시 `Authorization: Bearer <accessToken>` 헤더 포함
4. access 토큰 만료 시 `POST /api/auth/refresh` 로 재발급 (회전 방식: 기존 refresh 토큰은 즉시 폐기)
5. 로그아웃 시 `POST /api/auth/logout` 으로 refresh 토큰 폐기

토큰 규약:
- **access** (기본 15분): `typ=access`, `roles` 클레임 포함, 보호된 API 호출에 사용
- **refresh** (기본 14일): `typ=refresh`, DB(`refresh_tokens`)에 저장되어 폐기/회전 관리

## Environment variables
| 이름                    | 필수 | 기본값                                    | 설명                                       |
|-------------------------|------|-------------------------------------------|--------------------------------------------|
| `JWT_SECRET`            | 권장 | (개발용 플레이스홀더, 최소 32 bytes 필요) | JWT 서명용 시크릿. Base64 또는 raw 문자열  |
| `DB_PROFILE`            | 선택 | `h2`                                      | `h2` 또는 `postgres` (향후). 프로파일 선택 |
| `SPRING_PROFILES_ACTIVE`| 선택 | (none)                                    | Spring 표준 방식. 설정 시 `DB_PROFILE` 보다 우선. 테스트는 자동으로 `test` 적용 |
| `GOOGLE_CLIENT_ID`      | OAuth 사용 시 | -                            | Google OAuth2 Client ID. 미설정 시 Google 로그인 비활성화 |
| `GOOGLE_CLIENT_SECRET`  | OAuth 사용 시 | -                            | Google OAuth2 Client Secret                |
| `GITHUB_CLIENT_ID`      | OAuth 사용 시 | -                            | GitHub OAuth2 Client ID. 미설정 시 GitHub 로그인 비활성화 |
| `GITHUB_CLIENT_SECRET`  | OAuth 사용 시 | -                            | GitHub OAuth2 Client Secret                |
| `POSTGRES_URL`          | postgres 프로파일 사용 시 | `jdbc:postgresql://localhost:5432/authdb` | JDBC URL |
| `POSTGRES_USER`         | postgres 프로파일 사용 시 | `authservice`             | DB 사용자                                  |
| `POSTGRES_PASSWORD`     | postgres 프로파일 사용 시 | `authservice`             | DB 비밀번호                                |

프로덕션에서는 반드시 `JWT_SECRET` 을 충분히 길고 추측 불가능한 값으로 설정하세요.

## Architecture (요약)
```
Client ──HTTP──▶ SecurityFilterChain
                     │
                     ├── JwtAuthenticationFilter  (Authorization 헤더 파싱, SecurityContext 설정)
                     │        └── JwtTokenProvider (jjwt, HS256, typ=access|refresh)
                     │
                     ├── oauth2Login
                     │        ├── authorizationEndpoint  /api/auth/oauth2/authorize/{provider}
                     │        ├── redirectionEndpoint    /api/auth/oauth2/callback/{provider}
                     │        ├── CustomOAuth2UserService  (제공자 profile → OAuth2UserPrincipal)
                     │        └── OAuth2AuthenticationSuccessHandler (JWT JSON 반환)
                     │
                     └── AuthorizeHttpRequests
                              ├── /api/auth/**, /swagger-ui/**, /v3/api-docs/** → permitAll
                              ├── /api/admin/**                                  → hasRole("ADMIN")
                              └── 그 외                                          → authenticated

AuthController  ─▶ AuthService  ─▶ UserRepository / RoleRepository / RefreshTokenRepository
OAuthHandlers   ─▶ OAuthService ─▶ UserRepository / UserIdentityRepository / RefreshTokenRepository
                                         │
                                         └── PasswordEncoder (BCrypt, 로컬 가입만 사용)
```

- **Stateless**: 세션을 사용하지 않으며, 모든 요청은 JWT로 식별됩니다.
- **Refresh 회전**: `/api/auth/refresh` 호출 시 기존 refresh 토큰은 `revoked=true`로 표시되고 새 토큰이 발급됩니다.
- **RBAC**: `Role` 엔티티(`ROLE_USER`, `ROLE_ADMIN`)와 User ManyToMany. SecurityConfig 와 `@PreAuthorize` 이중 방어.
- **OAuth2**: `UserIdentity(provider, providerUserId)` 로 외부 계정을 내부 `User` 와 연결. 동일 이메일 자동 링크, 이메일 미제공 시 placeholder.

## Tasks
- [x] #1 프로젝트 스캐폴딩
- [x] #2 회원가입/로그인 + JWT
- [x] #3 RBAC
- [x] #4 OAuth2 소셜 로그인
- [x] #5 테스트 및 문서화
