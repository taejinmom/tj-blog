# PRD: Portfolio Platform MSA 통합

## 1. 개요 (Overview)

**portfolio-blog**를 메인 플랫폼으로, **auth-service**와 **tj-chat-service**를 독립 마이크로서비스로 통합하여 하나의 포트폴리오 플랫폼으로 운영한다.

### 목적
- 포트폴리오 사이트에 **인증(로그인/회원가입)** 과 **실시간 채팅** 기능을 추가
- MSA 아키텍처 경험을 포트폴리오로 보여줌
- Docker Compose 하나로 전체 플랫폼을 띄울 수 있는 구조

---

## 2. 현재 상태 (Current State)

### 프로젝트별 현황

| 서비스 | 기술 스택 | DB | 포트 | 프론트엔드 |
|--------|----------|-----|------|-----------|
| **portfolio-blog** | Spring Boot 3.2.5 | PostgreSQL (blogdb) | 8080 / 80 | React 19 + Vite 8 + Tailwind 4 |
| **auth-service** | Spring Boot 3.3.4 | H2 (in-memory) | 8080 | 없음 (API only) |
| **tj-chat-service** | Spring Boot 3.2.5 | PostgreSQL (chatdb) + Redis | 8081 / 3000 | React 18 + Vite 6 + Tailwind 3 |

### 현재 문제점
- 3개 프로젝트가 완전히 분리되어 있어 통합 실행 불가
- blog는 인증 없이 누구나 글 작성/삭제 가능
- chat-service는 자체 로그인(username/password)을 사용하며 JWT 미적용
- 프론트엔드가 blog, chat 각각 따로 존재

---

## 3. 목표 (Goals)

### 이번 작업으로 달성할 것
1. **통합 Docker Compose** — 루트 `docker-compose.yml` 하나로 전체 서비스 실행
2. **통합 프론트엔드** — portfolio-blog의 React 앱에 채팅 UI를 통합
3. **인증 연동** — auth-service의 JWT를 blog, chat 모두에서 사용
4. **Nginx 통합 라우팅** — 단일 진입점에서 각 서비스로 리버스 프록시
5. **서비스 간 통신** — Docker 내부 네트워크로 서비스 간 API 호출

### 비목표 (하지 않을 것)
- API Gateway (Spring Cloud Gateway 등) 도입 — Nginx로 충분
- 서비스 디스커버리 (Eureka 등) — Docker Compose 내 서비스명으로 대체
- 메시지 큐 (Kafka, RabbitMQ 등) 도입 — 현재 Redis Pub/Sub 유지
- chat-service 프론트엔드를 별도로 유지 — blog 프론트엔드에 통합
- OAuth2 소셜 로그인 설정 — 로컬 인증(이메일/패스워드)만 우선 연동
- CI/CD 파이프라인 변경 — 기존 Jenkins 유지, 추후 별도 작업

---

## 4. 아키텍처 (Architecture)

### 통합 후 서비스 구조

```
[Browser]
    |
    ▼
[Nginx :80] ─── 통합 진입점
    ├── /                → React SPA (portfolio-blog frontend)
    ├── /api/posts/**    → blog-backend :8080
    ├── /api/todos/**    → blog-backend :8080
    ├── /api/auth/**     → auth-service :8082
    ├── /api/users/**    → auth-service :8082
    ├── /api/admin/**    → auth-service :8082
    ├── /api/chat-rooms/**→ chat-backend :8081
    ├── /api/messages/** → chat-backend :8081
    └── /ws/**           → chat-backend :8081 (WebSocket)
```

### Docker Compose 서비스 목록

| 서비스 | 컨테이너명 | 내부 포트 | 외부 포트 | 의존성 |
|--------|-----------|----------|----------|--------|
| PostgreSQL | platform-db | 5432 | 5432 | - |
| Redis | platform-redis | 6379 | 6379 | - |
| auth-service | auth-backend | 8082 | - | PostgreSQL |
| blog-backend | blog-backend | 8080 | - | PostgreSQL |
| chat-backend | chat-backend | 8081 | - | PostgreSQL, Redis |
| frontend | platform-frontend | 80 | 80 | 모든 백엔드 |
| jenkins | blog-jenkins | 8080 | 9090 | - |

### 데이터베이스 구성

하나의 PostgreSQL 인스턴스에 DB를 분리하여 운영:

| 데이터베이스 | 사용 서비스 | 용도 |
|------------|-----------|------|
| `blogdb` | blog-backend | 게시글, 할일 |
| `authdb` | auth-service | 사용자, 인증, 토큰 |
| `chatdb` | chat-backend | 채팅방, 메시지, 읽음 |

---

## 5. 기능 요구사항 (Functional Requirements)

### 5.1 인증 통합

| 항목 | 상세 |
|------|------|
| 로그인/회원가입 | auth-service API를 portfolio 프론트엔드에서 호출 |
| JWT 적용 | blog-backend, chat-backend 모두 auth-service가 발급한 JWT 검증 |
| 토큰 저장 | 프론트엔드에서 accessToken을 메모리/localStorage에 저장 |
| 토큰 갱신 | refreshToken으로 자동 갱신 (Axios interceptor) |
| 로그아웃 | refreshToken 폐기 + 프론트엔드 토큰 삭제 |

### 5.2 블로그 인증 적용

| 기능 | 비로그인 | 로그인 사용자 | 관리자 |
|------|---------|-------------|--------|
| 글 목록 조회 | O | O | O |
| 글 상세 조회 | O | O | O |
| 글 작성 | X | X | O |
| 글 수정/삭제 | X | X | O |
| 할일 조회 | O | O | O |
| 할일 관리 | X | X | O |

### 5.3 채팅 통합

| 항목 | 상세 |
|------|------|
| UI 통합 | portfolio 프론트엔드에 `/chat` 경로로 채팅 페이지 추가 |
| 인증 연동 | chat-service의 자체 로그인을 제거하고 auth-service JWT 사용 |
| 사용자 연동 | auth-service의 User를 chat에서 참조 (JWT의 userId 활용) |
| WebSocket 인증 | STOMP 연결 시 JWT를 헤더로 전달하여 인증 |

### 5.4 프론트엔드 통합

| 라우트 | 페이지 | 인증 필요 |
|--------|--------|----------|
| `/` | 블로그 목록 (홈) | X |
| `/posts/:id` | 글 상세 | X |
| `/write` | 글 작성 | O (관리자) |
| `/write/:id` | 글 수정 | O (관리자) |
| `/todos` | 로드맵 | X |
| `/about` | 프로필 | X |
| `/chat` | 채팅 로비 | O |
| `/chat/:roomId` | 채팅방 | O |
| `/login` | 로그인 | X |
| `/signup` | 회원가입 | X |

### 5.5 Navbar 변경

- 기존: Blog, Roadmap, About
- 변경: Blog, Roadmap, Chat, About, **Login/Logout 버튼**
- 로그인 시: 사용자 이름 표시 + Logout 버튼

---

## 6. 기술 요구사항 (Technical Requirements)

### 6.1 auth-service 변경사항
- H2 → PostgreSQL (`authdb`) 전환
- 포트를 **8082**로 변경 (blog와 충돌 방지)
- CORS 설정에 통합 프론트엔드 origin 추가
- JWT 시크릿 키를 환경변수로 3개 서비스 공유

### 6.2 blog-backend 변경사항
- JWT 검증 필터 추가 (auth-service와 동일한 시크릿 키 사용)
- 글 작성/수정/삭제 API에 `ROLE_ADMIN` 권한 체크
- 조회 API는 인증 불필요 (기존 유지)

### 6.3 chat-backend 변경사항
- 자체 User 엔티티/로그인 제거
- JWT 검증 필터 추가
- WebSocket 연결 시 JWT 인증 핸들러 추가
- `senderId`를 JWT에서 추출하도록 변경
- DB를 독립 PostgreSQL → 공유 인스턴스의 `chatdb`로 변경

### 6.4 프론트엔드 변경사항
- chat-service의 React 컴포넌트를 portfolio 프론트엔드로 이관
- 인증 컨텍스트 (AuthContext/Provider) 추가
- Axios interceptor에 JWT 자동 첨부
- Protected Route 컴포넌트 추가
- 패키지 통합: `@stomp/stompjs`, `sockjs-client` 추가

### 6.5 Nginx 설정
- 경로 기반 리버스 프록시 설정
- WebSocket 업그레이드 지원 (`/ws`)
- SPA 라우팅 지원 (`try_files`)

### 6.6 공유 설정 (환경변수)

```yaml
# 공통
JWT_SECRET: ${JWT_SECRET}

# PostgreSQL (공유 인스턴스)
POSTGRES_HOST: platform-db
POSTGRES_PORT: 5432

# Redis
REDIS_HOST: platform-redis
REDIS_PORT: 6379
```

---

## 7. 구현 단계 (Implementation Phases)

### Phase 1: 인프라 통합
- [ ] 루트 `docker-compose.yml` 작성 (PostgreSQL, Redis, 3개 백엔드, 프론트엔드)
- [ ] PostgreSQL 초기화 스크립트 작성 (blogdb, authdb, chatdb 생성)
- [ ] Nginx 통합 설정 파일 작성
- [ ] auth-service PostgreSQL 프로파일 활성화 및 포트 변경

### Phase 2: 인증 연동
- [ ] blog-backend에 JWT 검증 필터 추가
- [ ] blog-backend API에 권한 체크 적용
- [ ] chat-backend 자체 로그인 제거 및 JWT 필터 추가
- [ ] chat-backend WebSocket JWT 인증 구현

### Phase 3: 프론트엔드 통합
- [ ] 로그인/회원가입 페이지 추가
- [ ] AuthContext/Provider 구현
- [ ] Axios interceptor (토큰 첨부, 자동 갱신)
- [ ] chat-service 컴포넌트 이관 및 통합
- [ ] Navbar 업데이트 (Chat, Login/Logout 추가)
- [ ] Protected Route 적용

### Phase 4: 테스트 및 정리
- [ ] Docker Compose로 전체 서비스 실행 테스트
- [ ] 인증 플로우 E2E 테스트 (가입 → 로그인 → 글 작성 → 채팅)
- [ ] WebSocket 연결 테스트
- [ ] README.md 업데이트

---

## 8. 성공 기준 (Success Criteria)

1. `docker compose up` 한 번으로 전체 플랫폼이 실행된다
2. 회원가입 → 로그인 후 JWT가 발급되고, 블로그/채팅 모두에서 인증이 동작한다
3. 비로그인 사용자는 블로그 조회만 가능하고, 글 작성은 관리자만 가능하다
4. 로그인 사용자는 채팅방 생성/참여/메시지 전송이 가능하다
5. WebSocket 실시간 채팅이 정상 동작한다
6. 각 서비스는 독립적으로 재시작해도 다른 서비스에 영향을 주지 않는다

---

## 9. 참고: 현재 API 엔드포인트 정리

### blog-backend (portfolio-blog)
```
GET/POST        /api/posts
GET/PUT/DELETE  /api/posts/{id}
GET             /api/posts/category/{category}
GET/POST        /api/todos
GET/PUT/DELETE  /api/todos/{id}
GET             /api/todos/phase/{phase}
GET             /api/todos/status/{status}
```

### auth-service
```
POST  /api/auth/signup
POST  /api/auth/login
POST  /api/auth/refresh
POST  /api/auth/logout
GET   /api/users/me
GET   /api/admin/users
```

### chat-backend (tj-chat-service)
```
GET/POST        /api/chat-rooms
GET             /api/chat-rooms/{id}
POST            /api/chat-rooms/{id}/join
POST            /api/chat-rooms/{id}/leave
GET             /api/chat-rooms/{id}/members
GET             /api/chat-rooms/{id}/messages
POST            /api/messages/{id}/read
GET             /api/service-info
WS              /ws (STOMP: /app/chat.send, /app/chat.typing)
```
