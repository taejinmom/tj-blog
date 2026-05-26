# TJ Blog - Portfolio Platform

학습 결과물을 정리하고 진행 중인 프로젝트를 관리하는 포트폴리오 플랫폼입니다.
마크다운 블로그, 프로젝트 로드맵, 실시간 채팅을 **마이크로서비스 아키텍처**로 구현했습니다.

---

## 주요 기능

- **블로그** — 마크다운 게시물 작성/편집/삭제(관리자), 카테고리 분류, 페이지네이션
- **프로젝트 로드맵** — Phase별 관리, 진행률 시각화, 상태 변경, 연동 서비스 바로가기(헬스체크)
- **인증/인가** — JWT 기반 회원가입·로그인, Access/Refresh 토큰, OAuth2 로그인, RBAC(`ROLE_USER`/`ROLE_ADMIN`)
- **실시간 채팅** — WebSocket(STOMP) + Redis Pub/Sub, 채팅방/메시지, 읽음 처리
- **다크 모드 / 반응형 UI**

---

## 아키텍처

```
┌───────────┐        ┌──────────────────────────────────────────────────────┐
│  Browser  │        │                     Docker Compose                     │
│ React SPA │──:80──▶│  platform-frontend (Nginx) ── 정적 SPA + /api 리버스 프록시 │
└───────────┘        │        │                                               │
                     │        ├─ /api/posts,/api/todos ─▶ blog-backend  :8080  │
                     │        ├─ /api/auth,/users,/admin ▶ auth-backend  :8082  │
                     │        └─ /api/chat-*,/ws        ─▶ chat-backend  :8081  │
                     │                                        │                │
                     │   platform-db (PostgreSQL :5432)  ◀────┤  blogdb        │
                     │     blogdb / authdb / chatdb           │  authdb        │
                     │   platform-redis (Redis :6379)    ◀────┘  chat pub/sub  │
                     └──────────────────────────────────────────────────────┘
```

---

## 기술 스택

### Backend (서비스별 독립 Spring Boot 앱)
| 서비스 | 포트 | 역할 | 핵심 기술 |
|--------|------|------|-----------|
| blog-backend | 8080 | 게시물 / 로드맵 | Spring Boot 3.2.5, Spring Data JPA, JWT 검증 |
| auth-backend | 8082 | 인증 / 인가 | Spring Security, JWT(Access/Refresh), OAuth2, RBAC |
| chat-backend | 8081 | 실시간 채팅 | Spring WebSocket(STOMP), Redis Pub/Sub |

공통: Java 17 · Gradle · PostgreSQL 15 · JWT(`io.jsonwebtoken` 0.12.6)

### Frontend
| 기술 | 용도 |
|------|------|
| React 19 + TypeScript | UI |
| Vite 8 | 빌드 / 개발 서버 |
| Tailwind CSS 4 | 스타일링 (다크 모드) |
| React Router 7 | 라우팅 |
| Axios | HTTP 클라이언트 (JWT 인터셉터, 토큰 자동 갱신) |
| @stomp/stompjs + sockjs-client | 채팅 WebSocket |
| react-markdown | 마크다운 렌더링 |

### Infrastructure
Docker / Docker Compose · Nginx(리버스 프록시 + SPA 서빙) · Redis

---

## 프로젝트 구조

```
tj-blog/
├── frontend/                     # React 19 + Vite SPA
│   └── src/
│       ├── api/                  # axios 클라이언트 (client.ts, chat.ts)
│       ├── context/AuthContext   # 인증 상태 / 토큰
│       ├── hooks/                # useChat, useWebSocket
│       ├── pages/                # Blog, TodoList, Auth, Chat, About
│       └── components/           # Navbar, Pagination, ProtectedRoute
├── services/
│   ├── blog/                     # 게시물(posts) + 로드맵(todos)
│   ├── auth/                     # 인증/인가 (JWT, OAuth2, RBAC) — 테스트 포함
│   └── chat/                     # WebSocket 채팅 + Redis
├── docker/
│   ├── nginx/default.conf        # 리버스 프록시 라우팅
│   └── postgres/init.sql         # authdb / chatdb 생성
├── docker-compose.yml
└── README.md
```

---

## 실행 방법 (Docker Compose)

### 사전 요구사항
- Docker 20.10+ / Docker Compose v2+
- (Windows) Docker Desktop + WSL2

### 전체 기동
```bash
# 운영용 JWT 시크릿을 환경변수로 지정 권장 (미지정 시 약한 기본값 사용)
export JWT_SECRET="$(openssl rand -base64 48)"

docker compose up -d --build
```

기동 순서: PostgreSQL(healthcheck) · Redis → blog/auth/chat backend → frontend(nginx)

### 상태 확인 & 접속
```bash
docker compose ps
```
| 서비스 | URL |
|--------|-----|
| 웹 (SPA + API 프록시) | http://localhost |
| blog API (직접) | http://localhost:8080/api |
| auth API (직접) | http://localhost:8082/api |
| chat API (직접) | http://localhost:8081/api |

### 페이지
| 경로 | 설명 |
|------|------|
| `/` | 블로그 목록 |
| `/posts/:id` | 게시물 상세 |
| `/write` | 게시물 작성 (관리자) |
| `/todos` | 프로젝트 로드맵 |
| `/chat` | 실시간 채팅 (로그인 필요) |
| `/login`, `/signup` | 인증 |
| `/about` | 소개 |

### 종료
```bash
docker compose down      # 데이터 유지
docker compose down -v    # 데이터 초기화
```

---

## 개발 환경 실행 (로컬)

```bash
# 1) 인프라만 Docker 로
docker compose up -d platform-db platform-redis

# 2) 각 백엔드 (별도 터미널)
cd services/blog && ./gradlew bootRun     # :8080
cd services/auth && ./gradlew bootRun     # :8082
cd services/chat && ./gradlew bootRun     # :8081

# 3) 프론트엔드
cd frontend && npm install && npm run dev  # :5173
```
`npm run dev` 의 `/api` 요청은 `vite.config.ts` 프록시가 각 백엔드로 분배합니다
(`VITE_BLOG_API` / `VITE_AUTH_API` / `VITE_CHAT_API` 환경변수로 타깃 변경 가능).

---

## API 요약

### blog (`:8080`)
| Method | URL | 권한 |
|--------|-----|------|
| GET | `/api/posts`, `/api/posts/{id}`, `/api/posts/category/{c}` | 공개 |
| POST/PUT/DELETE | `/api/posts/**` | `ROLE_ADMIN` |
| GET | `/api/todos`, `/api/todos/{id}`, `/api/todos/phase/{p}`, `/api/todos/status/{s}` | 공개 |
| POST/PUT/DELETE | `/api/todos/**` | `ROLE_ADMIN` |

### auth (`:8082`)
| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/auth/signup` | 회원가입 (`ROLE_USER`) |
| POST | `/api/auth/login` | 로그인 → Access/Refresh 토큰 |
| POST | `/api/auth/refresh` | 토큰 갱신 |
| GET | `/api/users/me` | 내 정보 (인증 필요) |
| GET | `/api/admin/**` | 관리자 (`ROLE_ADMIN`) |

> 관리자 권한은 가입 시 부여되지 않습니다. 글쓰기(`/write`) 테스트가 필요하면 DB(`authdb`)에서 해당 사용자에게 `ROLE_ADMIN` 을 부여한 뒤 재로그인하세요.

### chat (`:8081`)
| Method | URL | 설명 |
|--------|-----|------|
| GET/POST | `/api/chat-rooms` | 채팅방 (인증 필요) |
| GET | `/api/messages` | 메시지 (인증 필요) |
| GET | `/api/service-info` | 서비스 상태 |
| WS | `/ws` | STOMP WebSocket |

---

## 데이터베이스
| DB | 사용 서비스 |
|----|-------------|
| blogdb | blog-backend |
| authdb | auth-backend |
| chatdb | chat-backend |

접속: `docker exec -it platform-db psql -U blog -d blogdb` (PW: `blog1234`)
