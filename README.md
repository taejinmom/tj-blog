# TJ Blog - Portfolio Blog

학습 결과물을 정리하고, 진행 중인 프로젝트를 관리하는 포트폴리오 블로그 사이트입니다.
마크다운 기반 블로그 에디터, 프로젝트 로드맵 추적, 연동 서비스 바로가기 기능을 제공합니다.

---

## 주요 기능

- **블로그** — 마크다운 기반 게시물 작성/편집/삭제, 카테고리 분류, 페이지네이션
- **프로젝트 로드맵** — Phase별 프로젝트 관리, 진행률 시각화, 상태 변경 (Pending → In Progress → Completed)
- **서비스 바로가기** — 로드맵 내 프로젝트에 연결된 서비스가 있으면 실행 상태 표시 + 클릭 이동 (30초 헬스체크)
- **다크 모드** — 시스템 설정 연동
- **반응형 UI** — 모바일/데스크톱 대응
- **CI/CD** — Jenkins 파이프라인으로 자동 빌드/배포

---

## 아키텍처

```
┌─────────────┐       ┌──────────────────────────────────────────────┐
│   Browser   │       │            Docker Compose                    │
│             │       │                                              │
│  React App  │──80──▶│  ┌───────────┐    ┌──────────┐              │
│  (SPA)      │       │  │  Nginx    │    │ Backend  │              │
│             │       │  │ (Frontend)│───▶│ (Spring  │              │
└─────────────┘       │  │  :80      │    │  Boot)   │              │
                      │  └───────────┘    │  :8080   │              │
                      │    /api ─────────▶│          │              │
                      │                   └────┬─────┘              │
                      │                        │                    │
                      │                   ┌────┴─────┐              │
                      │                   │ Postgres │              │
                      │                   │  :5432   │              │
                      │                   └──────────┘              │
                      │                                              │
                      │  ┌───────────────┐                          │
                      │  │   Jenkins     │  CI/CD                   │
                      │  │   :9090       │                          │
                      │  └───────────────┘                          │
                      └──────────────────────────────────────────────┘
```

---

## 기술 스택

### Backend
| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 17 | 런타임 |
| Spring Boot | 3.2.5 | 웹 프레임워크 |
| Spring Data JPA | - | ORM / 데이터 접근 |
| PostgreSQL | 15 | 관계형 데이터베이스 |
| Gradle | 8.7 | 빌드 도구 |

### Frontend
| 기술 | 용도 |
|------|------|
| React 19 + TypeScript | UI 라이브러리 |
| Vite 8 | 빌드 도구 / 개발 서버 |
| Tailwind CSS 4 | 스타일링 (다크 모드) |
| React Router 7 | 클라이언트 라우팅 |
| react-markdown | 마크다운 렌더링 |
| Axios | HTTP 클라이언트 |

### Infrastructure
| 기술 | 용도 |
|------|------|
| Docker / Docker Compose | 컨테이너화 / 오케스트레이션 |
| Nginx | 리버스 프록시 / 정적 파일 서빙 |
| Jenkins | CI/CD 파이프라인 |

---

## 프로젝트 구조

```
portfolio-blog/
├── backend/
│   ├── src/main/java/com/taejin/blog/
│   │   ├── BlogApplication.java
│   │   ├── config/
│   │   │   └── WebConfig.java              # CORS 설정
│   │   └── domain/
│   │       ├── post/                        # 블로그 게시물
│   │       │   ├── Post.java                # Entity
│   │       │   ├── PostRequest.java         # DTO
│   │       │   ├── PostController.java
│   │       │   ├── PostService.java
│   │       │   └── PostRepository.java
│   │       └── todo/                        # 프로젝트 로드맵
│   │           ├── TodoItem.java            # Entity
│   │           ├── TodoRequest.java         # DTO
│   │           ├── TodoStatus.java          # Enum
│   │           ├── TodoController.java
│   │           ├── TodoService.java
│   │           └── TodoRepository.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── data.sql                         # 초기 데이터
│   ├── build.gradle
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── App.tsx                          # 라우팅 정의
│   │   ├── api/client.ts                    # Axios API 클라이언트
│   │   ├── types/index.ts                   # TypeScript 타입
│   │   ├── components/
│   │   │   ├── Navbar.tsx                   # 상단 네비게이션
│   │   │   └── Pagination.tsx               # 페이지네이션
│   │   └── pages/
│   │       ├── Blog/
│   │       │   ├── BlogList.tsx             # 게시물 목록 (6개/페이지)
│   │       │   ├── BlogDetail.tsx           # 게시물 상세 (마크다운 렌더링)
│   │       │   └── BlogEditor.tsx           # 게시물 작성/편집
│   │       ├── TodoList/
│   │       │   └── TodoListPage.tsx         # 로드맵 + 서비스 연결
│   │       └── About/
│   │           └── AboutPage.tsx            # 프로필 소개
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
├── docker/
│   ├── nginx/default.conf                   # Nginx 프록시 설정
│   └── jenkins/Dockerfile                   # Jenkins 이미지
├── docker-compose.yml
├── Jenkinsfile                              # CI/CD 파이프라인
└── README.md
```

---

## 실행 방법 (Docker Compose)

### 사전 요구사항

- **Docker** (20.10 이상)
- **Docker Compose** (v2.0 이상)
- **WSL2** (Windows 사용 시, Docker Desktop WSL Integration 활성화 필요)

```bash
docker --version
docker compose version
```

### 실행

#### 1. 프로젝트 클론

```bash
git clone https://github.com/taejinmom/tj-blog.git
cd tj-blog
```

#### 2. 전체 서비스 실행

```bash
docker compose up -d --build
```

시작 순서:
1. **PostgreSQL** — blogdb 생성, healthcheck 대기
2. **Backend** — Spring Boot (DB healthy 후 시작, data.sql로 초기 데이터 투입)
3. **Frontend** — Nginx + React SPA
4. **Jenkins** — CI/CD (선택)

#### 3. 상태 확인

```bash
docker compose ps
docker logs blog-backend    # 정상: "Started BlogApplication in X seconds"
```

#### 4. 접속

| 서비스 | URL | 설명 |
|--------|-----|------|
| 블로그 | http://localhost | React 포트폴리오 블로그 |
| 백엔드 API | http://localhost:8080/api | REST API 직접 접근 |
| Jenkins | http://localhost:9090 | CI/CD 대시보드 |

#### 5. 페이지 안내

| 경로 | 설명 |
|------|------|
| `/` | 블로그 게시물 목록 |
| `/posts/:id` | 게시물 상세 (마크다운 렌더링) |
| `/write` | 새 게시물 작성 |
| `/todos` | 프로젝트 로드맵 + 서비스 바로가기 |
| `/about` | 프로필 소개 |

#### 6. 종료

```bash
docker compose down          # 데이터 유지
docker compose down -v       # 데이터 초기화
```

---

## 개발 환경 실행 (로컬)

### 1. DB만 Docker로 실행

```bash
docker compose up -d db
```

### 2. Backend 실행

```bash
cd backend
./gradlew bootRun
```

### 3. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

Frontend 개발 서버(http://localhost:5173)에서 `/api` 요청은 Vite 프록시로 Backend(8080)에 전달됩니다.

---

## API 문서

### 게시물 API (`/api/posts`)

| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/posts` | 전체 조회 (생성일 역순) |
| `GET` | `/api/posts/{id}` | 상세 조회 |
| `GET` | `/api/posts/category/{category}` | 카테고리별 조회 |
| `POST` | `/api/posts` | 생성 `{ "title", "content", "category" }` |
| `PUT` | `/api/posts/{id}` | 수정 |
| `DELETE` | `/api/posts/{id}` | 삭제 |

### 로드맵 API (`/api/todos`)

| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/todos` | 전체 조회 (phase순, id순) |
| `GET` | `/api/todos/{id}` | 상세 조회 |
| `GET` | `/api/todos/phase/{phase}` | Phase별 조회 |
| `GET` | `/api/todos/status/{status}` | 상태별 조회 (`PENDING`, `IN_PROGRESS`, `COMPLETED`) |
| `POST` | `/api/todos` | 생성 `{ "title", "description", "phase", "status" }` |
| `PUT` | `/api/todos/{id}` | 수정 |
| `DELETE` | `/api/todos/{id}` | 삭제 |

---

## 데이터베이스

| 항목 | 값 |
|------|-----|
| Host | `localhost` |
| Port | `5432` |
| Database | `blogdb` |
| Username | `blog` |
| Password | `blog1234` |

```bash
# 접속
docker exec -it blog-db psql -U blog -d blogdb
```

### 테이블

**posts** — 블로그 게시물

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | 게시물 ID |
| title | VARCHAR | 제목 |
| content | TEXT | 내용 (마크다운) |
| category | VARCHAR | 카테고리 |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

**post_tags** — 게시물 태그 (ElementCollection)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| post_id | BIGINT FK | 게시물 ID |
| tag | VARCHAR | 태그 |

**todo_items** — 프로젝트 로드맵

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | 항목 ID |
| title | VARCHAR | 프로젝트명 |
| description | TEXT | 설명 |
| status | VARCHAR | PENDING / IN_PROGRESS / COMPLETED |
| phase | VARCHAR | 단계 (Phase0, Phase1, ...) |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

---

## 서비스 연동 (마이크로서비스)

로드맵 페이지에서 구현 완료된 프로젝트의 서비스로 바로 이동할 수 있습니다.

### 현재 연동된 서비스

| 프로젝트 | 서비스 URL | 헬스체크 URL |
|---------|-----------|-------------|
| 실시간 채팅 서비스 | http://localhost:3000 | http://localhost:8081/api/service-info |

### 새 서비스 연동 방법

`frontend/src/pages/TodoList/TodoListPage.tsx`의 `SERVICE_LINKS`에 추가:

```typescript
const SERVICE_LINKS: Record<string, ServiceLink> = {
  '채팅': {
    url: 'http://localhost:3000',
    healthUrl: 'http://localhost:8081/api/service-info',
    label: 'TJ Chat',
  },
  // 새 서비스 추가
  'API Gateway': {
    url: 'http://localhost:xxxx',
    healthUrl: 'http://localhost:xxxx/api/service-info',
    label: 'API Gateway',
  },
};
```

로드맵 항목의 제목에 키워드가 포함되면 자동으로 바로가기 버튼이 표시됩니다.

---

## CI/CD (Jenkins)

### Jenkins 접속

- URL: http://localhost:9090
- 초기 비밀번호: `docker exec blog-jenkins cat /var/jenkins_home/secrets/initialAdminPassword`

### 파이프라인 (Jenkinsfile)

1. **Checkout** — 소스 코드 체크아웃
2. **Build & Test** — Gradle 빌드 + 테스트 (Docker 에이전트)
3. **Docker Build** — Backend 이미지 빌드 및 태깅
4. **Deploy** — docker-compose로 backend 재배포
