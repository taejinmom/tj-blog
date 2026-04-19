# PRD Phase 1: 인프라 통합

## 1. 개요

portfolio-blog 모노레포의 3개 서비스(blog, auth, chat)를 **Docker Compose 하나로 실행**할 수 있는 인프라를 구성한다.

### 목표
- `docker compose up` 한 번으로 전체 플랫폼 실행
- 각 서비스는 독립 컨테이너로 분리
- PostgreSQL 1대에 DB 3개 분리 (blogdb, authdb, chatdb)
- Nginx가 단일 진입점으로 각 서비스에 라우팅

### 비목표
- 서비스 코드 변경 (인증 연동 등은 Phase 2에서 진행)
- 프론트엔드 통합 (Phase 3에서 진행)
- CI/CD 파이프라인 변경

---

## 2. 최종 아키텍처

```
                    ┌─────────────────────────────┐
                    │         Nginx (:80)          │
                    │       단일 진입점             │
                    └──┬──────────┬──────────┬─────┘
                       │          │          │
            ┌──────────┘    ┌─────┘    ┌─────┘
            ▼               ▼          ▼
     ┌─────────────┐ ┌──────────┐ ┌──────────┐
     │ blog (:8080)│ │auth(:8082│ │chat(:8081│
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

---

## 3. 컨테이너 구성

| 서비스 | 컨테이너명 | 이미지/빌드 | 내부 포트 | 외부 포트 | 의존성 |
|--------|-----------|------------|----------|----------|--------|
| PostgreSQL | platform-db | postgres:15 | 5432 | 5432 | - |
| Redis | platform-redis | redis:7-alpine | 6379 | 6379 | - |
| Blog Backend | blog-backend | services/blog | 8080 | - | platform-db |
| Auth Backend | auth-backend | services/auth | 8082 | - | platform-db |
| Chat Backend | chat-backend | services/chat | 8081 | - | platform-db, platform-redis |
| Frontend + Nginx | platform-frontend | frontend/ | 80 | 80 | 모든 백엔드 |

> 백엔드 포트는 외부 노출하지 않음. Nginx를 통해서만 접근.

---

## 4. 작업 목록

### 4.1 PostgreSQL 초기화 스크립트

**파일:** `docker/postgres/init.sql`

```sql
CREATE DATABASE authdb;
CREATE DATABASE chatdb;
-- blogdb는 docker-compose 환경변수로 자동 생성
```

PostgreSQL 컨테이너의 `/docker-entrypoint-initdb.d/`에 마운트하여 최초 실행 시 DB 자동 생성.

### 4.2 auth-service 설정 변경

**변경 사항:**
- 포트: 8080 → **8082** (blog와 충돌 방지)
- DB: H2 → PostgreSQL (authdb)
- `application.yml`에 PostgreSQL 프로파일 활성화

**환경변수:**
```yaml
SERVER_PORT: 8082
SPRING_DATASOURCE_URL: jdbc:postgresql://platform-db:5432/authdb
SPRING_DATASOURCE_USERNAME: blog
SPRING_DATASOURCE_PASSWORD: blog1234
SPRING_JPA_HIBERNATE_DDL_AUTO: update
```

### 4.3 chat-service 설정 변경

**변경 사항:**
- DB 호스트: `db` → `platform-db`
- Redis 호스트: `redis` → `platform-redis`

**환경변수:**
```yaml
DB_HOST: platform-db
DB_PORT: 5432
DB_NAME: chatdb
DB_USERNAME: blog
DB_PASSWORD: blog1234
REDIS_HOST: platform-redis
REDIS_PORT: 6379
```

### 4.4 blog-service 설정 변경

**변경 사항:**
- DB 호스트: `db` → `platform-db`

**환경변수:**
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://platform-db:5432/blogdb
SPRING_DATASOURCE_USERNAME: blog
SPRING_DATASOURCE_PASSWORD: blog1234
```

### 4.5 auth-service Dockerfile 작성

**파일:** `services/auth/Dockerfile`

현재 auth-service에는 Dockerfile이 없음. 다른 서비스(blog, chat)의 Dockerfile을 참고하여 작성.

```dockerfile
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.6 통합 docker-compose.yml

**파일:** 루트 `docker-compose.yml` (기존 파일 교체)

**서비스 정의:**

```yaml
version: '3.8'

services:
  platform-db:
    image: postgres:15
    container_name: platform-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: blogdb
      POSTGRES_USER: blog
      POSTGRES_PASSWORD: blog1234
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - platform-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U blog"]
      interval: 10s
      timeout: 5s
      retries: 5

  platform-redis:
    image: redis:7-alpine
    container_name: platform-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    networks:
      - platform-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  auth-backend:
    build: ./services/auth
    container_name: auth-backend
    restart: unless-stopped
    environment:
      SERVER_PORT: 8082
      SPRING_DATASOURCE_URL: jdbc:postgresql://platform-db:5432/authdb
      SPRING_DATASOURCE_USERNAME: blog
      SPRING_DATASOURCE_PASSWORD: blog1234
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_PROFILES_ACTIVE: postgres
    depends_on:
      platform-db:
        condition: service_healthy
    networks:
      - platform-network

  blog-backend:
    build: ./services/blog
    container_name: blog-backend
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://platform-db:5432/blogdb
      SPRING_DATASOURCE_USERNAME: blog
      SPRING_DATASOURCE_PASSWORD: blog1234
    depends_on:
      platform-db:
        condition: service_healthy
    networks:
      - platform-network

  chat-backend:
    build: ./services/chat
    container_name: chat-backend
    restart: unless-stopped
    environment:
      DB_HOST: platform-db
      DB_PORT: 5432
      DB_NAME: chatdb
      DB_USERNAME: blog
      DB_PASSWORD: blog1234
      REDIS_HOST: platform-redis
      REDIS_PORT: 6379
    depends_on:
      platform-db:
        condition: service_healthy
      platform-redis:
        condition: service_healthy
    networks:
      - platform-network

  platform-frontend:
    build: ./frontend
    container_name: platform-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./docker/nginx/default.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - blog-backend
      - auth-backend
      - chat-backend
    networks:
      - platform-network

volumes:
  postgres_data:

networks:
  platform-network:
    driver: bridge
```

### 4.7 Nginx 설정 변경

**파일:** `docker/nginx/default.conf`

```nginx
upstream blog-api {
    server blog-backend:8080;
}

upstream auth-api {
    server auth-backend:8082;
}

upstream chat-api {
    server chat-backend:8081;
}

server {
    listen 80;

    # React SPA
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # Blog API
    location /api/posts {
        proxy_pass http://blog-api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/todos {
        proxy_pass http://blog-api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # Auth API
    location /api/auth {
        proxy_pass http://auth-api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/users {
        proxy_pass http://auth-api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/admin {
        proxy_pass http://auth-api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # Chat API
    location /api/chat-rooms {
        proxy_pass http://chat-api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/messages {
        proxy_pass http://chat-api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/service-info {
        proxy_pass http://chat-api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket (Chat)
    location /ws {
        proxy_pass http://chat-api;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 86400;
    }
}
```

---

## 5. 파일 변경 요약

| 파일 | 작업 |
|------|------|
| `docker-compose.yml` | 전체 교체 (통합 구성) |
| `docker/postgres/init.sql` | **신규** (authdb, chatdb 생성) |
| `docker/nginx/default.conf` | 전체 교체 (멀티 서비스 라우팅) |
| `services/auth/Dockerfile` | **신규** (멀티스테이지 빌드) |
| `services/auth/src/main/resources/application.yml` | 포트 8082, PostgreSQL 설정 추가 |
| `services/chat/src/main/resources/application.yml` | DB/Redis 호스트명 환경변수 대응 확인 |
| `services/blog/src/main/resources/application.yml` | DB 호스트명 환경변수 대응 확인 |

---

## 6. 성공 기준

1. `docker compose up --build` 로 전체 서비스가 정상 기동된다
2. PostgreSQL에 blogdb, authdb, chatdb 3개 DB가 생성된다
3. 각 백엔드 healthcheck가 통과한다:
   - `curl http://localhost/api/posts` → blog 응답
   - `curl http://localhost/api/auth/signup` → auth 응답
   - `curl http://localhost/api/chat-rooms` → chat 응답
   - `ws://localhost/ws` → WebSocket 연결 성공
4. 각 서비스를 개별 재시작해도 다른 서비스에 영향 없음
5. 프론트엔드(React SPA)가 Nginx를 통해 정상 로딩됨

---

## 7. 주의사항

- auth-service의 `ddl-auto: update`는 개발 단계에서만 사용. 운영 시 Flyway 마이그레이션 전환 필요.
- DB 비밀번호(`blog1234`)는 개발용. 운영 환경에서는 `.env` 파일 또는 시크릿 관리 도구 사용.
- Jenkins 컨테이너는 이번 Phase에서 제외. 기존 설정 유지하되 docker-compose에 포함하지 않음 (필요 시 별도 profile로 추가).
