# Phase 2: 인증 연동

> **Status**: Draft
> **Author**: taejin
> **Created**: 2026-04-19
> **Depends on**: Phase 1 (Infra) ✅

---

## Context

Phase 1에서 blog, auth, chat 3개 서비스를 Docker Compose로 통합했다.
현재 문제:

- blog는 인증 없이 누구나 글을 작성/삭제할 수 있다
- chat은 자체 User 테이블과 로그인을 갖고 있어 auth-service와 이중 관리된다
- 서비스 간 인증 체계가 통일되어 있지 않다

## Goals

- auth-service가 발급한 JWT를 blog, chat 모두에서 검증한다
- blog 글 작성/수정/삭제는 관리자(ROLE_ADMIN)만 가능하다
- chat은 자체 로그인을 제거하고 JWT 인증으로 전환한다
- WebSocket 연결도 JWT로 인증한다

## Non-goals

- 프론트엔드 변경 (Phase 3에서 진행)
- OAuth2 소셜 로그인 설정
- API Gateway 도입
- 서비스 간 내부 통신 인증 (mTLS 등)

---

## User Stories

### US-1: 비로그인 사용자의 블로그 조회
```
AS A 비로그인 사용자
I WANT TO 블로그 글 목록과 상세를 볼 수 있다
SO THAT 로그인 없이도 콘텐츠를 소비할 수 있다
```
**Acceptance Criteria**
- `GET /api/posts`, `GET /api/posts/{id}` → 토큰 없이 200 응답
- `GET /api/todos`, `GET /api/todos/{id}` → 토큰 없이 200 응답

### US-2: 관리자의 블로그 관리
```
AS A 관리자 (ROLE_ADMIN)
I WANT TO 글을 작성/수정/삭제할 수 있다
SO THAT 블로그 콘텐츠를 관리할 수 있다
```
**Acceptance Criteria**
- `POST /api/posts` → ROLE_ADMIN JWT 필요, 없으면 401/403
- `PUT /api/posts/{id}` → ROLE_ADMIN JWT 필요
- `DELETE /api/posts/{id}` → ROLE_ADMIN JWT 필요
- `POST /api/todos` → ROLE_ADMIN JWT 필요
- `PUT /api/todos/{id}` → ROLE_ADMIN JWT 필요
- `DELETE /api/todos/{id}` → ROLE_ADMIN JWT 필요
- 일반 사용자(ROLE_USER) → 403

### US-3: 인증된 사용자의 채팅 이용
```
AS A 로그인한 사용자
I WANT TO JWT로 채팅 서비스를 이용할 수 있다
SO THAT 별도 회원가입 없이 채팅에 참여할 수 있다
```
**Acceptance Criteria**
- `GET /api/chat-rooms` → JWT 필요, 없으면 401
- `POST /api/chat-rooms` → JWT 필요
- WebSocket `/ws` 연결 → JWT 필요
- auth-service에서 가입한 계정으로 바로 채팅 가능
- chat의 자체 `/api/users` 로그인/회원가입 API는 제거

---

## Technical Design

### 공유 JWT 검증 구조

```
                  JWT_SECRET (동일한 시크릿 키 공유)
                       │
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
   ┌──────────┐  ┌──────────┐  ┌──────────┐
   │   auth   │  │   blog   │  │   chat   │
   │  발급+검증 │  │  검증만   │  │  검증만   │
   └──────────┘  └──────────┘  └──────────┘
```

- auth-service의 `JwtTokenProvider`가 HS256으로 서명
- blog, chat은 **동일한 시크릿 키**로 서명 검증만 수행
- JWT 파싱 라이브러리: `io.jsonwebtoken:jjwt` (auth-service와 동일)

### Task 1: blog-backend JWT 검증 추가

**변경 파일:**
```
services/blog/
├── build.gradle                          # jjwt 의존성 추가
└── src/main/java/com/taejin/blog/
    ├── config/
    │   └── SecurityConfig.java           # 신규: Spring Security 설정
    └── security/
        ├── JwtTokenProvider.java         # 신규: JWT 파싱/검증
        └── JwtAuthenticationFilter.java  # 신규: 요청별 JWT 검증 필터
```

**구현 상세:**

1. `build.gradle`에 의존성 추가
   ```groovy
   implementation 'org.springframework.boot:spring-boot-starter-security'
   implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
   runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
   runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
   ```

2. `SecurityConfig` 경로별 권한 설정
   ```
   permitAll:
     GET /api/posts/**
     GET /api/todos/**

   hasRole("ADMIN"):
     POST, PUT, DELETE /api/posts/**
     POST, PUT, DELETE /api/todos/**

   그 외: authenticated
   ```

3. `JwtTokenProvider` — auth-service와 동일한 시크릿으로 검증만 수행
   - `JWT_SECRET` 환경변수에서 키 로드
   - `validateToken(token)` → boolean
   - `getUserId(token)`, `getRoles(token)` → 클레임 추출

4. `JwtAuthenticationFilter` — OncePerRequestFilter
   - `Authorization: Bearer {token}` 헤더에서 토큰 추출
   - 유효하면 SecurityContext에 Authentication 설정
   - 무효하면 필터 통과 (permitAll 경로는 익명 접근 허용)

5. `application.yml`에 JWT 시크릿 추가
   ```yaml
   app:
     jwt:
       secret: ${JWT_SECRET:change-me-change-me-change-me-change-me-change-me-1234}
   ```

### Task 2: chat-backend 인증 전환

**변경 파일:**
```
services/chat/
├── build.gradle                          # jjwt 의존성 추가
└── src/main/java/com/taejin/chat/
    ├── config/
    │   ├── SecurityConfig.java           # 신규: Spring Security 설정
    │   ├── WebSocketConfig.java          # 수정: JWT 인증 인터셉터 추가
    │   └── WebSocketAuthInterceptor.java # 신규: STOMP 연결 시 JWT 검증
    ├── security/
    │   ├── JwtTokenProvider.java         # 신규: JWT 파싱/검증
    │   └── JwtAuthenticationFilter.java  # 신규: HTTP 요청 JWT 필터
    ├── controller/
    │   ├── UserController.java           # 삭제: 자체 로그인/회원가입 제거
    │   └── StompChatController.java      # 수정: senderId를 JWT에서 추출
    ├── dto/
    │   ├── UserRequest.java              # 삭제
    │   └── LoginRequest.java             # 삭제
    ├── entity/
    │   └── User.java                     # 수정: password 필드 제거, auth userId 참조
    └── service/
        └── UserService.java              # 수정: 자체 로그인 로직 제거
```

**구현 상세:**

1. JWT 검증 — blog와 동일한 `JwtTokenProvider`, `JwtAuthenticationFilter` 구조

2. `SecurityConfig` 경로별 권한
   ```
   permitAll:
     GET /api/service-info
     GET /actuator/health

   authenticated:
     /api/chat-rooms/**
     /api/messages/**
     /ws/**
   ```

3. WebSocket JWT 인증
   - `WebSocketAuthInterceptor` (ChannelInterceptor)
   - STOMP CONNECT 프레임의 `Authorization` 헤더에서 JWT 추출
   - 유효하면 `simpUser`에 인증 정보 설정
   - 무효하면 연결 거부

4. `StompChatController.sendMessage` 수정
   - `senderId`를 요청 바디가 아닌 JWT Principal에서 추출
   - 메시지 위조 방지

5. User 엔티티 변경
   - `password` 필드 제거
   - auth-service의 userId를 참조하는 구조로 전환
   - 자체 `/api/users` 회원가입/로그인 엔드포인트 제거

### Task 3: docker-compose 환경변수 통일

**변경 파일:**
```
docker-compose.yml   # JWT_SECRET 공유 환경변수 추가
```

```yaml
x-jwt-env: &jwt-env
  JWT_SECRET: ${JWT_SECRET:-change-me-change-me-change-me-change-me-change-me-1234}

services:
  auth-backend:
    environment:
      <<: *jwt-env
      # ... 기존 설정

  blog-backend:
    environment:
      <<: *jwt-env
      # ... 기존 설정

  chat-backend:
    environment:
      <<: *jwt-env
      # ... 기존 설정
```

---

## Test Scenarios

### blog-backend

| # | 시나리오 | 요청 | 기대 결과 |
|---|---------|------|----------|
| 1 | 비로그인 글 목록 조회 | `GET /api/posts` (토큰 없음) | 200 |
| 2 | 비로그인 글 작성 시도 | `POST /api/posts` (토큰 없음) | 401 |
| 3 | 일반 사용자 글 작성 시도 | `POST /api/posts` (ROLE_USER) | 403 |
| 4 | 관리자 글 작성 | `POST /api/posts` (ROLE_ADMIN) | 201 |
| 5 | 만료된 토큰 | `POST /api/posts` (expired JWT) | 401 |
| 6 | 비로그인 할일 조회 | `GET /api/todos` (토큰 없음) | 200 |
| 7 | 관리자 할일 관리 | `POST /api/todos` (ROLE_ADMIN) | 201 |

### chat-backend

| # | 시나리오 | 요청 | 기대 결과 |
|---|---------|------|----------|
| 1 | 비로그인 채팅방 조회 | `GET /api/chat-rooms` (토큰 없음) | 401 |
| 2 | 로그인 채팅방 조회 | `GET /api/chat-rooms` (JWT) | 200 |
| 3 | 로그인 채팅방 생성 | `POST /api/chat-rooms` (JWT) | 201 |
| 4 | WebSocket JWT 연결 | STOMP CONNECT + JWT | 연결 성공 |
| 5 | WebSocket 토큰 없음 | STOMP CONNECT (토큰 없음) | 연결 거부 |
| 6 | 자체 로그인 API 제거 확인 | `POST /api/users/login` | 404 |

### 통합 (Nginx 경유)

| # | 시나리오 | 요청 | 기대 결과 |
|---|---------|------|----------|
| 1 | auth에서 가입 → blog 글 작성 | signup → login → POST /api/posts | JWT 발급 → 403 (ROLE_USER) |
| 2 | 관리자로 전체 플로우 | admin login → POST /api/posts → GET /api/chat-rooms | 전부 성공 |
| 3 | auth 토큰으로 chat 접근 | login → GET /api/chat-rooms | 200 |

---

## Implementation Order

```
Task 1 (blog JWT)  ──→  Task 3 (docker-compose)  ──→  통합 테스트
Task 2 (chat 전환) ──┘
```

- Task 1, 2는 독립적이므로 병렬 진행 가능
- Task 3은 환경변수 통일이므로 마지막에 적용
- 통합 테스트는 전체 서비스를 Docker Compose로 띄워서 수행

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| JWT 시크릿 불일치로 검증 실패 | 전 서비스 인증 불가 | docker-compose YAML anchor로 단일 소스 관리 |
| chat User 엔티티 변경 시 기존 데이터 유실 | 채팅 이력 손실 | 개발 단계이므로 ddl-auto: update로 스키마 자동 변경, 데이터 초기화 허용 |
| blog에 Security 추가 시 CORS 이슈 | 프론트엔드 요청 차단 | SecurityConfig에서 CORS 설정 명시 |
