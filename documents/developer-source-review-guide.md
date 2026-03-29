# Developer Source Review Guide - TJ Blog

이 문서는 개발자가 소스 코드를 워크플로우 순서대로 따라가며 이해할 수 있도록 작성되었습니다.
각 기능별로 **사용자 액션 → Frontend → Backend → DB** 흐름을 따라갑니다.

---

## 목차

1. [프로젝트 진입점](#1-프로젝트-진입점)
2. [블로그 게시물 목록 조회](#2-블로그-게시물-목록-조회)
3. [블로그 게시물 상세 조회](#3-블로그-게시물-상세-조회)
4. [블로그 게시물 작성/편집](#4-블로그-게시물-작성편집)
5. [프로젝트 로드맵 조회 + 상태 변경](#5-프로젝트-로드맵-조회--상태-변경)
6. [서비스 바로가기 (마이크로서비스 연동)](#6-서비스-바로가기-마이크로서비스-연동)
7. [설정 및 인프라](#7-설정-및-인프라)
8. [CI/CD 파이프라인](#8-cicd-파이프라인)

---

## 1. 프로젝트 진입점

### Backend

```
backend/src/main/java/com/taejin/blog/BlogApplication.java
```

Spring Boot 메인 클래스. `@SpringBootApplication`으로 컴포넌트 스캔 시작.

**설정:**
```
backend/src/main/resources/application.yml
```
- DB 연결 (PostgreSQL), JPA 설정 (`ddl-auto: update`)
- `defer-datasource-initialization: true` — Hibernate DDL 후 data.sql 실행
- `sql.init.mode: always` — 매 시작 시 data.sql 실행

**초기 데이터:**
```
backend/src/main/resources/data.sql
```
- 로드맵 항목 (todo_items) 초기 데이터 INSERT (WHERE NOT EXISTS로 중복 방지)

### Frontend

```
frontend/src/main.tsx    → ReactDOM 렌더링 진입점
frontend/src/App.tsx     → 라우팅 정의
```

**라우팅 구조:**
```
/              → BlogList.tsx       (게시물 목록)
/posts/:id     → BlogDetail.tsx     (게시물 상세)
/write         → BlogEditor.tsx     (새 게시물 작성)
/write/:id     → BlogEditor.tsx     (게시물 편집)
/todos         → TodoListPage.tsx   (프로젝트 로드맵)
/about         → AboutPage.tsx      (프로필)
```

**공통 컴포넌트:**
```
components/Navbar.tsx     → 상단 네비게이션 (Blog, TodoList, About)
components/Pagination.tsx → 페이지네이션 (BlogList에서 사용)
```

---

## 2. 블로그 게시물 목록 조회

### 시퀀스

```
브라우저 접속(/) → BlogList.tsx → api/client.ts → PostController → PostService → PostRepository → DB
```

### 소스 추적

#### 1단계: 페이지 로드 (Frontend)

```
frontend/src/pages/Blog/BlogList.tsx
```
- `useEffect` → `postApi.getAll()` 호출
- 응답을 `posts` 상태에 저장
- 6개씩 페이지네이션: `posts.slice((page-1)*6, page*6)`
- 게시물 카드: 카테고리 배지 + 제목 + 미리보기(마크다운 기호 제거, 150자) + 날짜
- `<Link to={/posts/${id}}>` → 클릭 시 상세 페이지로 이동

#### 2단계: API 호출 (Frontend)

```
frontend/src/api/client.ts
```
- `postApi.getAll()` → `GET /api/posts` → 응답 `.data` 추출

#### 3단계: API 수신 (Backend)

```
backend/.../domain/post/PostController.java
```
- `@GetMapping` → `postService.findAll()`
- `@CrossOrigin(origins = "*")` — CORS 허용

#### 4단계: 비즈니스 로직 (Backend)

```
backend/.../domain/post/PostService.java
```
- `findAll()` → `postRepository.findAllByOrderByCreatedAtDesc()` — 최신순 정렬

#### 5단계: 데이터 접근 (Backend)

```
backend/.../domain/post/PostRepository.java
```
- `findAllByOrderByCreatedAtDesc()` — Spring Data JPA 메서드 네이밍 쿼리

#### Entity & DTO

```
backend/.../domain/post/Post.java
```
- `@Entity @Table(name = "posts")`
- 필드: id, title, content(TEXT), category, tags(ElementCollection), createdAt, updatedAt
- `@PrePersist` / `@PreUpdate` — 타임스탬프 자동 설정

```
backend/.../domain/post/PostRequest.java
```
- title(@NotBlank), content(@NotBlank), category, tags

---

## 3. 블로그 게시물 상세 조회

### 시퀀스

```
목록에서 클릭 → BlogDetail.tsx → api/client.ts → PostController → PostService → DB
```

### 소스 추적

```
frontend/src/pages/Blog/BlogDetail.tsx
```
- `useParams()` → URL에서 `id` 추출
- `useEffect` → `postApi.getById(Number(id))` 호출
- `<ReactMarkdown>{post.content}</ReactMarkdown>` — **마크다운 렌더링**
- Edit 버튼 → `navigate('/write/${post.id}')`
- Delete 버튼 → `confirm()` → `postApi.delete(post.id)` → 목록으로 이동

**핵심 포인트:**
- `react-markdown` 라이브러리로 마크다운 → HTML 변환
- `prose dark:prose-invert` Tailwind 클래스로 마크다운 스타일링

---

## 4. 블로그 게시물 작성/편집

### 시퀀스

```
작성: "New Post" 클릭 → BlogEditor.tsx → api/client.ts → PostController.create → DB
편집: "Edit" 클릭 → BlogEditor.tsx → (기존 데이터 로드) → PostController.update → DB
```

### 소스 추적

```
frontend/src/pages/Blog/BlogEditor.tsx
```
- `useParams()` → `id` 유무로 작성/편집 분기: `const isEdit = Boolean(id)`
- 편집 모드: `useEffect` → `postApi.getById()` → 폼에 데이터 채움
- `handleSubmit`:
  - 편집: `postApi.update(id, { title, content, category })` → 상세 페이지로 이동
  - 작성: `postApi.create({ title, content, category })` → 상세 페이지로 이동
- 입력 필드: 제목, 카테고리, 콘텐츠(textarea, `font-mono` 마크다운 에디터)

#### Backend 처리

```
backend/.../domain/post/PostController.java
```
- `@PostMapping` → `postService.create(request)` → Post 엔티티 생성 → DB 저장
- `@PutMapping("/{id}")` → `postService.update(id, request)` → 기존 엔티티 수정 → DB 저장

```
backend/.../domain/post/PostService.java
```
- `create()`: PostRequest → Post 엔티티 변환 → `postRepository.save()`
- `update()`: `findById()` → 필드 업데이트 → `@PreUpdate`로 updatedAt 자동 갱신

---

## 5. 프로젝트 로드맵 조회 + 상태 변경

### 시퀀스

```
/todos 접속 → TodoListPage.tsx → api/client.ts → TodoController → TodoService → DB
상태 클릭 → todoApi.update → TodoController.update → TodoService.update → DB
```

### 소스 추적

```
frontend/src/pages/TodoList/TodoListPage.tsx
```

**조회:**
- `useEffect` → `todoApi.getAll()` 호출
- `grouped` — todos를 `phase`별로 그룹화: `reduce<Record<string, TodoItem[]>>`
- `phases` — 그룹 키를 정렬하여 Phase0, Phase1, ... 순서로 표시
- 진행률 바: `completedCount / totalCount * 100`

**상태 변경:**
- `NEXT_STATUS` 매핑: PENDING → IN_PROGRESS → COMPLETED → PENDING (순환)
- 원형 버튼 클릭 → `handleStatusChange(todo)` → `todoApi.update(id, {..., status: nextStatus})`
- 응답으로 `setTodos` 업데이트 → 리렌더링

**상태별 UI:**
- PENDING: 빈 원
- IN_PROGRESS: 파란 점이 있는 원
- COMPLETED: 체크 마크가 있는 초록 원 + 제목에 취소선

#### Backend 처리

```
backend/.../domain/todo/TodoController.java
```
- `@GetMapping` → `todoService.findAll()` — phase순, id순
- `@PutMapping("/{id}")` → `todoService.update(id, request)`

```
backend/.../domain/todo/TodoService.java
```
- `findAll()`: `todoRepository.findAllByOrderByPhaseAscIdAsc()`
- `update()`: 기존 엔티티 조회 → title, description, status, phase 업데이트 → 저장

```
backend/.../domain/todo/TodoItem.java
```
- `@Entity @Table(name = "todo_items")`
- `@Enumerated(EnumType.STRING) status` — TodoStatus enum

```
backend/.../domain/todo/TodoStatus.java
```
- `PENDING`, `IN_PROGRESS`, `COMPLETED`

---

## 6. 서비스 바로가기 (마이크로서비스 연동)

### 시퀀스

```
TodoListPage 로드 → SERVICE_LINKS에서 title 키워드 매칭
  → ServiceButton 컴포넌트 렌더링 → fetch(healthUrl) → 상태 표시
  → 클릭 → 새 탭에서 서비스 URL 열기
```

### 소스 추적

```
frontend/src/pages/TodoList/TodoListPage.tsx
```

**서비스 매핑:**
```typescript
const SERVICE_LINKS: Record<string, ServiceLink> = {
  '채팅': {
    url: 'http://localhost:3000',        // 서비스 URL
    healthUrl: 'http://localhost:8081/api/service-info',  // 헬스체크
    label: 'TJ Chat',                   // 버튼 라벨
  },
};
```

**매칭 로직:**
```typescript
function findServiceLink(title: string): ServiceLink | null {
  for (const [keyword, link] of Object.entries(SERVICE_LINKS)) {
    if (title.includes(keyword)) return link;  // title에 키워드 포함 여부
  }
  return null;
}
```

- 각 todo 항목 렌더링 시 `findServiceLink(todo.title)` 호출
- 매칭되면 `<ServiceButton link={serviceLink} />` 렌더링

**ServiceButton 컴포넌트:**
- `useEffect` → `fetch(healthUrl, { mode: 'no-cors' })` — 30초 간격 헬스체크
- 성공: 초록색 점 (pulse 애니메이션) + 활성 스타일
- 실패: 빨간색 점 + 비활성 스타일
- `<a href={url} target="_blank">` — 새 탭에서 서비스 열기

**새 서비스 추가 시:**
`SERVICE_LINKS` 객체에 항목 추가 → 로드맵 제목에 키워드가 포함되면 자동 표시

---

## 7. 설정 및 인프라

### Backend 설정

| 파일 | 역할 |
|------|------|
| `application.yml` | DB 연결, JPA(ddl-auto: update), data.sql 실행 설정 |
| `config/WebConfig.java` | CORS 전역 설정 (`/api/**`, 모든 출처 허용) |
| `data.sql` | 로드맵 초기 데이터 (WHERE NOT EXISTS로 중복 방지) |

**핵심 설정:**
```yaml
spring:
  jpa:
    defer-datasource-initialization: true  # DDL 이후 data.sql 실행
    hibernate:
      ddl-auto: update                     # 자동 스키마 생성/업데이트
  sql:
    init:
      mode: always                         # 매 시작 시 data.sql 실행
```

### Frontend 설정

| 파일 | 역할 |
|------|------|
| `vite.config.ts` | `/api` 프록시 → `http://was:8080` (개발 서버) |
| `index.css` | Tailwind import + `@theme` 커스텀 컬러 (primary 등) |

### Nginx 설정

```
docker/nginx/default.conf
```
- `location /api` → `proxy_pass http://backend:8080` (REST API 프록시)
- `location /` → `try_files $uri /index.html` (SPA 라우팅)

### Docker Compose 서비스

| 서비스 | 이미지 | 포트 | 의존성 |
|--------|-------|------|--------|
| db | postgres:15 | 5432 | - |
| backend | ./backend (Multi-stage) | 8080 | db (healthy) |
| frontend | ./frontend (Multi-stage) | 80 | backend |
| jenkins | ./docker/jenkins | 9090, 50000 | - |

---

## 8. CI/CD 파이프라인

### 소스 추적

```
Jenkinsfile
```

**Stage 1: Checkout**
- `checkout scm` — 소스 코드 체크아웃

**Stage 2: Build & Test**
- Docker 에이전트: `gradle:8.7-jdk17`
- `gradle clean build --no-daemon`
- JUnit 테스트 결과 수집

**Stage 3: Docker Build**
- `docker build -t blog-backend:${BUILD_NUMBER} ./backend`
- `docker tag blog-backend:${BUILD_NUMBER} blog-backend:latest`

**Stage 4: Deploy**
- `docker-compose up -d --build backend` — 백엔드 컨테이너 재배포

**Jenkins 설정:**
```
docker/jenkins/Dockerfile
```
- Jenkins 이미지에 Docker CLI 포함 (호스트 Docker 소켓 마운트)

---

## 부록: 소스 리뷰 체크리스트

### Phase 1: 전체 구조 파악 (5분)

- [ ] `App.tsx` → 라우팅 구조 (5개 경로)
- [ ] `Navbar.tsx` → 네비게이션 항목 (Blog, TodoList, About)
- [ ] `application.yml` → DB, JPA, data.sql 설정
- [ ] `docker-compose.yml` → 4개 서비스 구성

### Phase 2: 블로그 CRUD (10분)

- [ ] `api/client.ts` → `postApi` 전체 엔드포인트
- [ ] `BlogList.tsx` → 목록 조회 + 페이지네이션 (6개/페이지)
- [ ] `BlogDetail.tsx` → 마크다운 렌더링 (`react-markdown`) + 삭제
- [ ] `BlogEditor.tsx` → 작성/편집 분기 (`isEdit = Boolean(id)`)
- [ ] `PostController.java` → REST API 5개
- [ ] `PostService.java` → CRUD 로직 + 트랜잭션
- [ ] `Post.java` → Entity (posts + post_tags)

### Phase 3: 로드맵 (5분)

- [ ] `TodoListPage.tsx` → phase별 그룹화, 진행률 바, 상태 순환 변경
- [ ] `TodoController.java` → REST API 7개
- [ ] `TodoService.java` → CRUD + 기본 상태 PENDING 설정
- [ ] `TodoItem.java` → Entity + TodoStatus enum

### Phase 4: 서비스 연동 (3분)

- [ ] `TodoListPage.tsx` → `SERVICE_LINKS` 매핑 구조
- [ ] `findServiceLink()` → 제목 키워드 매칭
- [ ] `ServiceButton` → 헬스체크 (30초) + 상태 표시 + 바로가기

### Phase 5: CI/CD (3분)

- [ ] `Jenkinsfile` → 4단계 파이프라인
- [ ] `docker/jenkins/Dockerfile` → Jenkins + Docker CLI
