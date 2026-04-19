# Phase 1: 인프라 통합

> **Status**: Done ✅
> **Author**: taejin
> **Created**: 2026-04-19
> **Depends on**: None

---

## Context

portfolio-blog, auth-service, tj-chat-service 3개의 독립 프로젝트가 각각 별도의 docker-compose를 갖고 따로 실행되고 있다.
현재 문제:

- 서비스마다 별도의 PostgreSQL 컨테이너를 띄워야 한다
- 프론트엔드가 blog, chat 각각 따로 존재한다
- 3개 서비스를 동시에 실행하려면 수동으로 각각 docker compose up 해야 한다
- 포트 충돌 관리를 수동으로 해야 한다

## Goals

- `docker compose up` 한 번으로 전체 플랫폼을 실행한다
- 각 서비스는 독립 컨테이너로 분리한다
- PostgreSQL 1대에 DB 3개를 분리한다 (blogdb, authdb, chatdb)
- Nginx가 단일 진입점(:80)으로 경로별 라우팅한다

## Non-goals

- 서비스 코드 변경 (인증 연동 등은 Phase 2에서 진행)
- 프론트엔드 통합 (Phase 3에서 진행)
- CI/CD 파이프라인 변경

---

## User Stories

### US-1: 개발자의 원커맨드 실행
```
AS A 개발자
I WANT TO docker compose up 한 번으로 전체 플랫폼을 실행할 수 있다
SO THAT 개발 환경 세팅에 시간을 쓰지 않는다
```
**Acceptance Criteria**
- `docker compose up --build` → 6개 컨테이너(db, redis, blog, auth, chat, frontend) 모두 기동
- 각 컨테이너 STATUS가 `Up` 또는 `healthy`
- 기존 docker volume이 없는 상태에서도 정상 기동 (init.sql로 DB 자동 생성)

### US-2: Nginx 통합 라우팅
```
AS A 프론트엔드
I WANT TO 단일 호스트(:80)로 모든 API를 호출할 수 있다
SO THAT 서비스별 포트를 알 필요 없다
```
**Acceptance Criteria**
- `GET http://localhost/api/posts` → blog-backend 200 응답
- `POST http://localhost/api/auth/signup` → auth-backend 응답
- `GET http://localhost/api/chat-rooms` → chat-backend 200 응답
- `ws://localhost/ws` → WebSocket 연결 성공
- `GET http://localhost/` → React SPA 로딩 (index.html)

### US-3: 서비스 독립성
```
AS A 개발자
I WANT TO 개별 서비스를 재시작해도 다른 서비스에 영향이 없다
SO THAT 서비스별 독립 배포가 가능하다
```
**Acceptance Criteria**
- `docker compose restart blog-backend` → auth, chat 서비스 정상 유지
- `docker compose restart auth-backend` → blog, chat 서비스 정상 유지
- `docker compose restart chat-backend` → blog, auth 서비스 정상 유지

---

## Technical Design

### 아키텍처

```
                    ┌─────────────────────────────┐
                    │         Nginx (:80)          │
                    │       단일 진입점             │
                    └──┬──────────┬──────────┬─────┘
                       │          │          │
            ┌──────────┘    ┌─────┘    ┌─────┘
            ▼               ▼          ▼
     ┌─────────────┐ ┌──────────┐ ┌──────────┐
     │ blog (:8080)│ │auth(:8082)│ │chat(:8081)│
     │ Spring Boot │ │Spr. Boot │ │Spr. Boot │
     └──────┬──────┘ └────┬─────┘ └──┬───┬───┘
            │              │          │   │
            └──────┬───────┘──────────┘   │
                   ▼                      ▼
          ┌──────────────┐        ┌────────────┐
          │ PostgreSQL   │        │   Redis    │
          │ (:5432)      │        │  (:6379)   │
          │ blogdb       │        └────────────┘
          │ authdb       │
          │ chatdb       │
          └──────────────┘
```

### 컨테이너 구성

| 서비스 | 컨테이너명 | 이미지/빌드 | 내부 포트 | 외부 포트 | 의존성 |
|--------|-----------|------------|----------|----------|--------|
| PostgreSQL | platform-db | postgres:15 | 5432 | 5432 | - |
| Redis | platform-redis | redis:7-alpine | 6379 | 6379 | - |
| Blog Backend | blog-backend | services/blog | 8080 | - | platform-db |
| Auth Backend | auth-backend | services/auth | 8082 | - | platform-db |
| Chat Backend | chat-backend | services/chat | 8081 | - | platform-db, platform-redis |
| Frontend + Nginx | platform-frontend | frontend/ | 80 | 80 | 모든 백엔드 |

> 백엔드 포트는 외부 노출하지 않음. Nginx를 통해서만 접근.

### Task 1: PostgreSQL 초기화 스크립트

**신규 파일:** `docker/postgres/init.sql`

```sql
CREATE DATABASE authdb;
CREATE DATABASE chatdb;
-- blogdb는 POSTGRES_DB 환경변수로 자동 생성
```

- PostgreSQL 컨테이너의 `/docker-entrypoint-initdb.d/`에 마운트
- volume이 비어있는 최초 실행 시에만 init 스크립트 실행됨
- 기존 volume이 있으면 스킵되므로, DB 추가 시 `docker compose down -v` 필요

### Task 2: auth-service 설정 변경

**변경 파일:**
```
services/auth/
├── Dockerfile                                  # 신규: 멀티스테이지 빌드
└── src/main/resources/
    └── application.yml                         # 수정: 포트 8082
```

**구현 상세:**

1. Dockerfile 작성 (blog, chat과 동일 구조)
   - Build: `gradle:8.7-jdk17` → `gradle bootJar`
   - Run: `eclipse-temurin:17-jre` → `java -jar app.jar`

2. `application.yml` 포트 변경
   ```yaml
   server:
     port: ${SERVER_PORT:8082}
   ```

3. docker-compose 환경변수로 PostgreSQL 프로파일 활성화
   ```yaml
   SPRING_PROFILES_ACTIVE: postgres
   POSTGRES_URL: jdbc:postgresql://platform-db:5432/authdb
   POSTGRES_USER: blog
   POSTGRES_PASSWORD: blog1234
   ```

### Task 3: blog/chat 서비스 설정 변경

**변경 파일:**
```
services/blog/src/main/resources/application.yml   # DB URL 환경변수 대응
services/chat/src/main/resources/application.yml   # 포트 8081, DB/Redis 호스트명 확인
```

**구현 상세:**

1. blog `application.yml` — 하드코딩 DB URL을 환경변수 대응으로 변경
   ```yaml
   spring:
     datasource:
       url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/blogdb}
       username: ${SPRING_DATASOURCE_USERNAME:blog}
       password: ${SPRING_DATASOURCE_PASSWORD:blog1234}
   ```

2. chat `application.yml` — 포트 변경 (이미 환경변수 대응 되어있음)
   ```yaml
   server:
     port: ${SERVER_PORT:8081}
   ```

### Task 4: 통합 docker-compose.yml

**변경 파일:** 루트 `docker-compose.yml` (전체 교체)

- 기존 blog 단독 구성 → 6개 서비스 통합 구성
- healthcheck 기반 의존성 관리
- 단일 네트워크 (`platform-network`)
- 단일 PostgreSQL volume

### Task 5: Nginx 설정 변경

**변경 파일:** `docker/nginx/default.conf` (전체 교체)

- 기존: `location /api/` → blog-backend 단일 프록시
- 변경: 경로별 upstream 분기
  - `/api/posts`, `/api/todos` → blog-backend:8080
  - `/api/auth`, `/api/users`, `/api/admin` → auth-backend:8082
  - `/api/chat-rooms`, `/api/messages`, `/api/service-info` → chat-backend:8081
  - `/ws` → chat-backend:8081 (WebSocket upgrade)
  - `/` → React SPA (try_files)

---

## Test Scenarios

### 컨테이너 기동

| # | 시나리오 | 명령어 | 기대 결과 |
|---|---------|--------|----------|
| 1 | 전체 빌드 & 실행 | `docker compose up --build` | 6개 컨테이너 모두 Up |
| 2 | DB healthcheck | `docker compose ps` | platform-db: healthy |
| 3 | Redis healthcheck | `docker compose ps` | platform-redis: healthy |
| 4 | 클린 상태 기동 | `docker compose down -v && docker compose up` | init.sql로 authdb, chatdb 생성됨 |

### API 라우팅 (Nginx 경유)

| # | 시나리오 | 요청 | 기대 결과 |
|---|---------|------|----------|
| 1 | Blog 글 조회 | `GET http://localhost/api/posts` | 200 |
| 2 | Auth 회원가입 | `POST http://localhost/api/auth/signup` (JSON body) | 200/201 |
| 3 | Auth 로그인 | `POST http://localhost/api/auth/login` (JSON body) | 200 (JWT 발급) |
| 4 | Chat 채팅방 목록 | `GET http://localhost/api/chat-rooms` | 200 |
| 5 | Frontend SPA | `GET http://localhost/` | 200 (index.html) |
| 6 | SPA 라우팅 | `GET http://localhost/todos` | 200 (index.html, try_files) |
| 7 | WebSocket | `ws://localhost/ws` | STOMP 연결 성공 |

### 서비스 독립성

| # | 시나리오 | 명령어 | 기대 결과 |
|---|---------|--------|----------|
| 1 | blog만 재시작 | `docker compose restart blog-backend` | auth, chat API 정상 응답 |
| 2 | auth만 재시작 | `docker compose restart auth-backend` | blog, chat API 정상 응답 |
| 3 | chat만 재시작 | `docker compose restart chat-backend` | blog, auth API 정상 응답 |

---

## Implementation Order

```
Task 1 (init.sql)  ──→  Task 4 (docker-compose.yml)  ──→  빌드 & 실행 테스트
Task 2 (auth 설정) ──┘         ↑
Task 3 (blog/chat) ──────────┘
Task 5 (nginx)     ──────────┘
```

- Task 1~3, 5는 독립적이므로 병렬 진행 가능
- Task 4는 모든 설정이 확정된 후 작성
- 최종 테스트는 `docker compose down -v && docker compose up --build`

---

## File Changes Summary

| 파일 | 작업 |
|------|------|
| `docker-compose.yml` | 전체 교체 (6개 서비스 통합) |
| `docker/postgres/init.sql` | **신규** (authdb, chatdb 자동 생성) |
| `docker/nginx/default.conf` | 전체 교체 (경로별 멀티 서비스 라우팅) |
| `services/auth/Dockerfile` | **신규** (멀티스테이지 빌드) |
| `services/auth/src/main/resources/application.yml` | 포트 8082 변경 |
| `services/chat/src/main/resources/application.yml` | 포트 8081 변경 |
| `services/blog/src/main/resources/application.yml` | DB URL 환경변수 대응 |

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| 기존 PostgreSQL volume이 있으면 init.sql 스킵 | authdb, chatdb 미생성 → auth, chat 기동 실패 | `docker compose down -v`로 volume 초기화 후 재시작 |
| auth-service의 `ddl-auto: update`로 운영 스키마 변경 | 의도하지 않은 스키마 변경 | 개발 단계에서만 사용, 운영 시 Flyway 전환 |
| DB 비밀번호 하드코딩 | 보안 취약 | 개발 전용, 운영 시 `.env` 파일 또는 시크릿 매니저 사용 |
| Jenkins 컨테이너 제외 | CI/CD 중단 | 기존 설정 보존, 필요 시 docker-compose profile로 추가 |
