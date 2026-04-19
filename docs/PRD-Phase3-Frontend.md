# Phase 3: 프론트엔드 통합

> **Status**: Draft
> **Author**: taejin
> **Created**: 2026-04-19
> **Depends on**: Phase 2 (Auth) 

---

## Context

Phase 2에서 blog, chat 백엔드에 JWT 인증을 적용했다.
현재 문제:

- 프론트엔드에 로그인/회원가입 UI가 없어 인증 API를 사용할 수 없다
- 채팅 UI가 `frontend/chat-reference/`에 별도로 존재하며 blog 프론트엔드와 통합되지 않았다
- Navbar에 Chat, Login/Logout 메뉴가 없다
- API 호출 시 JWT를 수동으로 붙여야 한다

## Goals

- blog 프론트엔드에 로그인/회원가입 페이지를 추가한다
- 채팅 UI를 blog 프론트엔드에 통합한다
- 인증 상태를 전역으로 관리한다 (AuthContext)
- API 호출 시 JWT를 자동 첨부하고, 만료 시 자동 갱신한다
- 인증이 필요한 페이지는 ProtectedRoute로 보호한다

## Non-goals

- 채팅 UI 디자인 리뉴얼 (기존 chat-reference 컴포넌트 재사용)
- OAuth2 소셜 로그인 버튼 (로컬 이메일/패스워드만 우선)
- 회원 프로필 페이지
- 다국어 지원

---

## User Stories

### US-1: 회원가입
```
AS A 신규 사용자
I WANT TO 이메일과 비밀번호로 회원가입할 수 있다
SO THAT 플랫폼의 인증이 필요한 기능을 이용할 수 있다
```
**Acceptance Criteria**
- `/signup` 페이지에서 이메일, 비밀번호, 표시 이름 입력 후 가입
- 가입 성공 → 로그인 페이지로 이동 + 성공 메시지
- 이메일 중복 → 에러 메시지 표시
- 비밀번호 미입력 등 유효성 검증 실패 → 에러 메시지 표시

### US-2: 로그인/로그아웃
```
AS A 가입된 사용자
I WANT TO 로그인하여 인증된 상태로 서비스를 이용할 수 있다
SO THAT 채팅 참여, 관리자 기능 등을 사용할 수 있다
```
**Acceptance Criteria**
- `/login` 페이지에서 이메일, 비밀번호 입력 후 로그인
- 로그인 성공 → 홈(`/`)으로 이동, Navbar에 사용자 이름 + Logout 버튼 표시
- 로그인 실패 → "이메일 또는 비밀번호가 올바르지 않습니다" 에러 메시지
- Logout 클릭 → 토큰 삭제, 홈으로 이동, Navbar에 Login 버튼 복원
- 브라우저 새로고침 → 로그인 상태 유지 (localStorage의 토큰으로 복원)

### US-3: 자동 토큰 관리
```
AS A 로그인한 사용자
I WANT TO 토큰 만료를 신경 쓰지 않고 서비스를 이용할 수 있다
SO THAT 15분마다 재로그인하지 않아도 된다
```
**Acceptance Criteria**
- API 호출 시 accessToken 자동 첨부 (Axios interceptor)
- 401 응답 수신 → refreshToken으로 자동 갱신 → 원래 요청 재시도
- refreshToken도 만료된 경우 → 로그아웃 처리 + 로그인 페이지로 이동

### US-4: 채팅 UI 통합
```
AS A 로그인한 사용자
I WANT TO blog 사이트 내에서 채팅 기능을 이용할 수 있다
SO THAT 별도의 사이트로 이동하지 않아도 된다
```
**Acceptance Criteria**
- `/chat` → 채팅 로비 (채팅방 목록 + 생성)
- `/chat/:roomId` → 채팅방 (실시간 메시지, 읽음 표시)
- 비로그인 상태에서 `/chat` 접근 → `/login`으로 리다이렉트
- WebSocket 연결 시 JWT를 헤더로 전달
- 기존 chat-reference 컴포넌트의 핵심 기능 유지 (실시간 메시지, 읽음 표시, 채팅방 생성)

### US-5: Navbar 업데이트
```
AS A 사용자
I WANT TO Navbar에서 모든 주요 기능에 접근할 수 있다
SO THAT 사이트 탐색이 직관적이다
```
**Acceptance Criteria**
- 비로그인: Blog, Roadmap, About, **Login** 버튼
- 로그인: Blog, Roadmap, **Chat**, About, **{displayName}**, **Logout** 버튼
- 현재 페이지에 해당하는 메뉴 항목 활성화 표시
- 모바일 반응형 유지

---

## Technical Design

### 디렉토리 구조 (변경 후)

```
frontend/src/
├── App.tsx                          # 수정: 라우팅 추가
├── api/
│   └── client.ts                    # 수정: JWT interceptor 추가
├── context/
│   └── AuthContext.tsx              # 신규: 인증 상태 전역 관리
├── components/
│   ├── Navbar.tsx                   # 수정: Chat, Login/Logout 추가
│   ├── Pagination.tsx
│   └── ProtectedRoute.tsx          # 신규: 인증 필요 라우트 가드
├── pages/
│   ├── Auth/
│   │   ├── LoginPage.tsx           # 신규
│   │   └── SignupPage.tsx          # 신규
│   ├── Blog/
│   │   ├── BlogList.tsx
│   │   ├── BlogDetail.tsx
│   │   └── BlogEditor.tsx
│   ├── Chat/
│   │   ├── ChatPage.tsx            # 신규: 채팅 메인 (로비 + 채팅방)
│   │   ├── ChatRoomList.tsx        # chat-reference에서 이관
│   │   ├── ChatRoom.tsx            # chat-reference에서 이관
│   │   ├── MessageList.tsx         # chat-reference에서 이관
│   │   ├── MessageInput.tsx        # chat-reference에서 이관
│   │   └── CreateRoomModal.tsx     # chat-reference에서 이관
│   ├── TodoList/
│   │   └── TodoListPage.tsx
│   └── About/
│       └── AboutPage.tsx
├── hooks/
│   ├── useWebSocket.ts             # chat-reference에서 이관
│   └── useChat.ts                  # chat-reference에서 이관
├── services/
│   └── websocket.ts                # chat-reference에서 이관
└── types/
    └── index.ts                    # 수정: Auth, Chat 타입 추가
```

### Task 1: AuthContext 구현

**신규 파일:** `frontend/src/context/AuthContext.tsx`

```typescript
interface AuthState {
  user: User | null;          // { id, email, displayName, roles }
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;         // 초기 토큰 복원 중
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
}
```

**동작:**
- 마운트 시 localStorage에서 accessToken, refreshToken 복원
- accessToken을 디코딩하여 user 정보 추출 (jwtDecode)
- login 성공 → 토큰 저장 + user 상태 설정
- logout → 토큰 삭제 + user 상태 초기화 + `POST /api/auth/logout` 호출

**의존성 추가:** `jwt-decode` (토큰 디코딩용)

### Task 2: Axios Interceptor 수정

**변경 파일:** `frontend/src/api/client.ts`

**Request Interceptor:**
```typescript
// 모든 요청에 Authorization 헤더 자동 첨부
config.headers.Authorization = `Bearer ${accessToken}`;
```

**Response Interceptor:**
```typescript
// 401 수신 시:
// 1. refreshToken으로 POST /api/auth/refresh
// 2. 새 토큰 저장
// 3. 실패한 요청 재시도
// 4. refresh도 실패 → logout 처리
```

**주의:** 동시에 여러 요청이 401을 받는 경우, refresh를 한 번만 호출하도록 큐잉 처리

### Task 3: 로그인/회원가입 페이지

**신규 파일:**
- `frontend/src/pages/Auth/LoginPage.tsx`
- `frontend/src/pages/Auth/SignupPage.tsx`

**LoginPage:**
- 이메일, 비밀번호 입력 폼
- "계정이 없으신가요? 회원가입" 링크 → `/signup`
- AuthContext의 `login()` 호출
- 에러 메시지 표시 영역

**SignupPage:**
- 이메일, 비밀번호, 표시 이름 입력 폼
- "이미 계정이 있으신가요? 로그인" 링크 → `/login`
- AuthContext의 `signup()` 호출
- 성공 → `/login`으로 이동 + 성공 토스트

**디자인:** 기존 blog 스타일(Tailwind, 다크모드) 통일

### Task 4: 채팅 컴포넌트 이관

**소스:** `frontend/chat-reference/src/` → `frontend/src/pages/Chat/`

**이관 대상:**
| chat-reference 파일 | 이관 위치 | 수정 사항 |
|---------------------|----------|----------|
| `ChatRoomList.tsx` | `pages/Chat/ChatRoomList.tsx` | API 호출에 JWT 자동 첨부 |
| `ChatRoom.tsx` | `pages/Chat/ChatRoom.tsx` | senderId를 AuthContext에서 가져오기 |
| `MessageList.tsx` | `pages/Chat/MessageList.tsx` | 최소 변경 |
| `MessageInput.tsx` | `pages/Chat/MessageInput.tsx` | 최소 변경 |
| `CreateRoomModal.tsx` | `pages/Chat/CreateRoomModal.tsx` | creatorId를 AuthContext에서 가져오기 |
| `LoginPage.tsx` | 삭제 (자체 로그인 제거) | Auth 페이지로 대체 |
| `Layout.tsx` | 삭제 | blog의 App.tsx 라우팅으로 대체 |
| `useWebSocket.ts` | `hooks/useWebSocket.ts` | STOMP 연결 시 JWT 헤더 추가 |
| `useChat.ts` | `hooks/useChat.ts` | 최소 변경 |
| `websocket.ts` | `services/websocket.ts` | JWT 헤더 전달 |
| `api.ts` | 삭제 | 기존 `api/client.ts` 사용 |
| `types/index.ts` | `types/index.ts`에 병합 | Chat 관련 타입 추가 |

**ChatPage.tsx (신규):**
- 채팅 메인 레이아웃 (사이드바 + 채팅방)
- `ChatRoomList` + `ChatRoom` 조합
- chat-reference의 `Layout.tsx` 역할 대체

### Task 5: ProtectedRoute 컴포넌트

**신규 파일:** `frontend/src/components/ProtectedRoute.tsx`

```typescript
// 비인증 사용자 → /login 리다이렉트
// 인증 로딩 중 → 로딩 스피너
// 인증 완료 → children 렌더링
```

### Task 6: 라우팅 & Navbar 수정

**변경 파일:**
- `frontend/src/App.tsx` — 라우트 추가
- `frontend/src/components/Navbar.tsx` — 메뉴 항목 추가

**라우트 구성:**
```typescript
/              → BlogList          (public)
/posts/:id     → BlogDetail        (public)
/write         → BlogEditor        (protected, ROLE_ADMIN)
/write/:id     → BlogEditor        (protected, ROLE_ADMIN)
/todos         → TodoListPage      (public)
/about         → AboutPage         (public)
/chat          → ChatPage          (protected)
/chat/:roomId  → ChatPage          (protected)
/login         → LoginPage         (public, 로그인 시 / 리다이렉트)
/signup        → SignupPage        (public, 로그인 시 / 리다이렉트)
```

---

## Test Scenarios

### 회원가입 & 로그인

| # | 시나리오 | 조작 | 기대 결과 |
|---|---------|------|----------|
| 1 | 정상 회원가입 | `/signup`에서 폼 입력 후 제출 | `/login`으로 이동 + 성공 메시지 |
| 2 | 중복 이메일 가입 | 동일 이메일로 재가입 | 에러 메시지 표시 |
| 3 | 정상 로그인 | `/login`에서 폼 입력 후 제출 | `/`로 이동 + Navbar에 사용자명 표시 |
| 4 | 로그인 실패 | 잘못된 비밀번호 입력 | 에러 메시지 표시 |
| 5 | 로그아웃 | Navbar의 Logout 클릭 | `/`로 이동 + Login 버튼 복원 |
| 6 | 새로고침 유지 | 로그인 후 F5 | 로그인 상태 유지 |

### 토큰 자동 관리

| # | 시나리오 | 조건 | 기대 결과 |
|---|---------|------|----------|
| 1 | 자동 첨부 | 로그인 후 API 호출 | Authorization 헤더에 JWT 포함 |
| 2 | 자동 갱신 | accessToken 만료 후 API 호출 | refreshToken으로 갱신 → 요청 재시도 성공 |
| 3 | 전체 만료 | refreshToken도 만료 후 API 호출 | 로그아웃 → `/login`으로 이동 |

### 채팅 통합

| # | 시나리오 | 조작 | 기대 결과 |
|---|---------|------|----------|
| 1 | 비로그인 채팅 접근 | URL로 `/chat` 직접 접근 | `/login`으로 리다이렉트 |
| 2 | 채팅방 목록 | 로그인 후 Chat 메뉴 클릭 | 채팅방 목록 표시 |
| 3 | 채팅방 생성 | "새 채팅방" 버튼 → 이름 입력 | 채팅방 생성 + 목록에 추가 |
| 4 | 메시지 전송 | 채팅방 입장 → 메시지 입력 → Enter | 실시간 메시지 표시 |
| 5 | 읽음 표시 | 다른 사용자가 메시지 읽음 | 읽지 않은 수 감소 |

### Navbar

| # | 시나리오 | 상태 | 기대 결과 |
|---|---------|------|----------|
| 1 | 비로그인 메뉴 | 로그아웃 상태 | Blog, Roadmap, About, Login |
| 2 | 로그인 메뉴 | 로그인 상태 | Blog, Roadmap, Chat, About, {이름}, Logout |
| 3 | 모바일 반응형 | 화면 축소 | 햄버거 메뉴 정상 동작 |

---

## Implementation Order

```
Task 1 (AuthContext)  ──→  Task 3 (Login/Signup)  ──→  Task 6 (라우팅 & Navbar)
         │                                                      ↑
         └──→  Task 2 (Axios Interceptor)                       │
                                                                │
Task 4 (채팅 이관)  ──→  Task 5 (ProtectedRoute)  ──────────────┘
```

- Task 1이 선행 (모든 인증 관련 기능의 기반)
- Task 2, 3은 Task 1 완료 후 병렬 가능
- Task 4는 독립적으로 병렬 진행 가능
- Task 5, 6은 모든 페이지와 컨텍스트가 준비된 후 마지막

---

## Dependencies

**npm 추가 패키지:**
```json
{
  "jwt-decode": "^4.0.0",
  "@stomp/stompjs": "^7.0.0",
  "sockjs-client": "^1.6.1"
}
```

**chat-reference 이관 후:** `frontend/chat-reference/` 디렉토리 삭제

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| chat-reference의 React 18 컴포넌트가 React 19에서 비호환 | 빌드 실패 또는 런타임 에러 | 이관 시 React 19 호환성 확인, 필요 시 API 변경 적용 |
| Tailwind 버전 차이 (chat: v3, blog: v4) | 스타일 깨짐 | blog의 Tailwind v4 기준으로 클래스 통일 |
| STOMP/SockJS 패키지가 기존 번들 사이즈 증가 | 초기 로딩 느려짐 | 채팅 페이지 lazy loading (React.lazy + Suspense) |
| 토큰 갱신 중 동시 요청 Race condition | 여러 번 refresh 호출 | interceptor에서 refresh 큐잉 (isRefreshing 플래그) |
| localStorage 토큰 XSS 노출 | 토큰 탈취 위험 | 개발 단계 허용, 운영 시 httpOnly cookie 전환 검토 |
