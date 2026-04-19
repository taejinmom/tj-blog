# TJ Chat Service

실시간 채팅 기능을 제공하는 마이크로서비스입니다. Spring Boot 백엔드와 React 프론트엔드로 구성되어 있으며, WebSocket(STOMP)을 통한 실시간 메시징, Redis Pub/Sub을 활용한 다중 서버 메시지 브로드캐스트, PostgreSQL 기반의 데이터 영속성을 지원합니다.

향후 Master 웹서비스와 통합하여 전체 시스템의 채팅 모듈로 활용할 수 있도록 설계되었습니다.

---

## 주요 기능

- 회원가입 / 로그인
- 그룹 채팅 / 1:1 채팅 (DIRECT)
- WebSocket(STOMP) 기반 실시간 메시지 전송/수신
- Redis Pub/Sub을 통한 다중 서버 메시지 브로드캐스트
- 카카오톡 스타일 읽음 표시 (안 읽은 멤버 수 실시간 감소)
- 채팅방 참가 / 나가기 (멤버 0명 시 자동 삭제)
- 접속 유저 목록 (온라인/오프라인)
- 알림 토스트
- 다크 테마 반응형 UI

---

## 아키텍처

```
┌─────────────┐       ┌──────────────────────────────────────────────┐
│   Browser   │       │            Docker Compose                    │
│             │       │                                              │
│  React App  │─3000─▶│  ┌───────────┐    ┌──────────┐              │
│  (SPA)      │       │  │  Nginx    │    │ Backend  │              │
│             │       │  │ (Frontend)│───▶│ (Spring  │              │
└─────────────┘       │  │  :80      │    │  Boot)   │              │
                      │  └───────────┘    │  :8080   │              │
                      │    /api, /ws ────▶│          │              │
                      │                   └────┬─────┘              │
                      │                   ┌────┴─────┐              │
                      │              ┌────┴───┐ ┌────┴───┐         │
                      │              │ Redis  │ │Postgres│         │
                      │              │ :6379  │ │ :5432  │         │
                      │              └────────┘ └────────┘         │
                      └──────────────────────────────────────────────┘
```

**요청 흐름:**
1. 브라우저가 Nginx(외부 `:3000` → 내부 `:80`)에 접속
2. 정적 파일(React SPA) 요청은 Nginx가 직접 서빙
3. `/api/*` REST 요청은 Backend(`:8080`)로 프록시
4. `/ws` WebSocket 연결은 Backend로 프록시 (Upgrade 헤더 포함)
5. Backend는 PostgreSQL에 데이터를 저장하고, Redis Pub/Sub으로 실시간 메시지를 브로드캐스트

---

## 기술 스택

### Backend
| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 17 | 런타임 |
| Spring Boot | 3.2.5 | 웹 프레임워크 |
| Spring WebSocket | - | STOMP 기반 실시간 통신 |
| Spring Data JPA | - | ORM / 데이터 접근 |
| Spring Data Redis | - | Redis 연동 / Pub/Sub |
| Spring Actuator | - | 헬스체크 / 모니터링 |
| PostgreSQL | 15 | 관계형 데이터베이스 |
| Redis | 7 | 메시지 브로커 |
| Gradle | 8.7 | 빌드 도구 |

### Frontend
| 기술 | 용도 |
|------|------|
| React 19 + TypeScript | UI 라이브러리 |
| Vite 8 | 빌드 도구 / 개발 서버 |
| Tailwind CSS 4 | 스타일링 (다크 테마) |
| @stomp/stompjs + SockJS | WebSocket 클라이언트 |
| Axios | HTTP 클라이언트 |
| React Router 7 | 클라이언트 라우팅 |

### Infrastructure
| 기술 | 용도 |
|------|------|
| Docker / Docker Compose | 컨테이너화 / 오케스트레이션 |
| Nginx | 리버스 프록시 / 정적 파일 서빙 |

---

## 프로젝트 구조

```
tj-chat-service/
├── backend/
│   ├── src/main/java/com/taejin/chat/
│   │   ├── ChatApplication.java            # 메인 애플리케이션
│   │   ├── config/
│   │   │   ├── WebSocketConfig.java         # STOMP WebSocket 설정
│   │   │   ├── RedisConfig.java             # Redis Pub/Sub 설정
│   │   │   ├── CorsConfig.java              # CORS 설정
│   │   │   └── WebSocketEventListener.java  # WebSocket 이벤트 리스너
│   │   ├── controller/
│   │   │   ├── UserController.java          # 사용자 REST API
│   │   │   ├── ChatRoomController.java      # 채팅방 REST API
│   │   │   ├── MessageController.java       # 메시지 REST API
│   │   │   ├── StompChatController.java     # STOMP 메시지 핸들러
│   │   │   ├── ServiceInfoController.java   # 서비스 정보 API
│   │   │   └── GlobalExceptionHandler.java  # 전역 예외 처리
│   │   ├── dto/                             # 요청/응답 DTO
│   │   ├── entity/                          # JPA 엔티티 (User, ChatRoom, ChatRoomMember, ChatMessage, ReadReceipt)
│   │   ├── repository/                      # Spring Data 리포지토리
│   │   └── service/                         # 비즈니스 로직 (Redis Pub/Sub 포함)
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── build.gradle
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/                      # React 컴포넌트
│   │   │   ├── LoginPage.tsx                # 로그인/회원가입
│   │   │   ├── Layout.tsx                   # 전체 레이아웃 (사이드바 + 메인)
│   │   │   ├── ChatRoomList.tsx             # 채팅방 목록
│   │   │   ├── ChatRoom.tsx                 # 채팅 화면 (나가기 포함)
│   │   │   ├── MessageList.tsx              # 메시지 목록 (날짜 구분)
│   │   │   ├── MessageItem.tsx              # 메시지 (읽음 표시 포함)
│   │   │   ├── MessageInput.tsx             # 메시지 입력
│   │   │   ├── CreateRoomModal.tsx           # 채팅방 생성 모달
│   │   │   ├── UserList.tsx                 # 접속 유저 목록
│   │   │   └── NotificationToast.tsx        # 알림 토스트
│   │   ├── hooks/
│   │   │   ├── useWebSocket.ts              # WebSocket 연결/구독 관리
│   │   │   └── useChat.ts                   # 채팅 로직 + 읽음 업데이트
│   │   ├── services/
│   │   │   ├── api.ts                       # REST API 클라이언트 (Axios)
│   │   │   └── websocket.ts                 # STOMP WebSocket 서비스
│   │   └── types/index.ts                   # TypeScript 타입 정의
│   ├── nginx.conf                           # Nginx 설정 (프로덕션)
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
├── docker/nginx/default.conf                # Nginx 설정 (참조용)
├── docker-compose.yml
├── .gitignore
└── README.md
```

---

## 실행 방법 (Docker Compose)

### 사전 요구사항

- **Docker** (20.10 이상)
- **Docker Compose** (v2.0 이상)
- **WSL2** (Windows 사용 시, Docker Desktop WSL Integration 활성화 필요)

```bash
# 설치 확인
docker --version
docker compose version
```

> **Windows 사용자:** Docker Desktop → Settings → Resources → WSL Integration에서 사용 중인 WSL 배포판을 활성화해야 합니다.

### 실행

#### 1. 프로젝트 클론

```bash
git clone https://github.com/taejinmom/tj-chat-service.git
cd tj-chat-service
```

#### 2. Docker Compose로 전체 서비스 실행

```bash
docker compose up -d --build
```

이 명령어는 다음 서비스를 순서대로 시작합니다:
1. **PostgreSQL** — chatdb 데이터베이스 자동 생성, healthcheck 완료 대기
2. **Redis** — 메시지 브로커, healthcheck 완료 대기
3. **Backend** — Spring Boot (DB/Redis healthy 후 시작)
4. **Frontend** — Nginx + React SPA (Backend 시작 후 실행)

> 첫 빌드 시 Gradle 의존성 다운로드와 npm install로 시간이 걸릴 수 있습니다.

#### 3. 상태 확인

```bash
# 전체 서비스 상태 확인
docker compose ps

# 백엔드 로그 확인 (정상 기동: "Started ChatApplication in X seconds")
docker logs chat-backend

# 전체 로그 실시간 확인
docker compose logs -f
```

#### 4. 접속

| 서비스 | URL | 설명 |
|--------|-----|------|
| 채팅 UI | http://localhost:3000 | React 채팅 애플리케이션 |
| 백엔드 API | http://localhost:8081/api | REST API 직접 접근 |
| 헬스체크 | http://localhost:8081/actuator/health | 서비스 헬스 상태 |
| 서비스 정보 | http://localhost:8081/api/service-info | 서비스 메타데이터 |

> **포트 안내:** tj-blog 서비스와 동시 실행을 위해 기본 포트 대신 위 포트를 사용합니다.
> 단독 실행 시 `docker-compose.yml`에서 포트를 `80:80`, `8080:8080` 등으로 변경할 수 있습니다.

#### 5. 사용 방법

1. http://localhost:3000 접속
2. **회원가입** 탭에서 아이디/닉네임/비밀번호 입력 후 가입
3. 로그인 후 **새 채팅방** 버튼으로 채팅방 생성 (그룹/1:1 선택)
4. 채팅방 선택 → 메시지 전송 (Enter)
5. 다른 브라우저/시크릿 탭에서 다른 계정으로 로그인하여 실시간 채팅 테스트

#### 6. 종료

```bash
# 서비스 중지 (데이터 유지)
docker compose down

# 서비스 중지 + 데이터 초기화 (DB 볼륨 삭제)
docker compose down -v

# 서비스 중지 + 이미지까지 삭제 (완전 정리)
docker compose down -v --rmi all
```

---

## 개발 환경 실행 방법 (로컬)

로컬 개발 시에는 DB와 Redis만 Docker로 실행하고, Backend와 Frontend는 직접 실행합니다.

### 사전 요구사항

- Java 17+
- Node.js 20+
- Docker (DB, Redis용)

### 1. 인프라 서비스 실행

```bash
docker compose up -d db redis
```

### 2. Backend 실행

```bash
cd backend
./gradlew bootRun
```

Backend는 http://localhost:8080 에서 실행됩니다.

### 3. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

Frontend 개발 서버는 http://localhost:3000 에서 실행됩니다.
Vite의 프록시 설정으로 `/api`, `/ws` 요청이 Backend로 자동 전달됩니다.

---

## API 문서

### 사용자 API

| Method | URL | 설명 | Request Body |
|--------|-----|------|-------------|
| `POST` | `/api/users` | 회원가입 | `{ "username", "nickname", "password" }` |
| `POST` | `/api/users/login` | 로그인 (상태 ONLINE으로 변경) | `{ "username", "password" }` |
| `GET` | `/api/users` | 전체 사용자 조회 | - |
| `GET` | `/api/users/{id}` | 사용자 상세 조회 | - |

### 채팅방 API

| Method | URL | 설명 | Request Body |
|--------|-----|------|-------------|
| `GET` | `/api/chat-rooms` | 전체 채팅방 조회 | - |
| `POST` | `/api/chat-rooms` | 채팅방 생성 | `{ "name", "type": "GROUP\|DIRECT", "creatorId" }` |
| `GET` | `/api/chat-rooms/{id}` | 채팅방 상세 조회 | - |
| `POST` | `/api/chat-rooms/{id}/join` | 채팅방 참가 | `{ "userId" }` |
| `POST` | `/api/chat-rooms/{id}/leave` | 채팅방 나가기 (0명 시 자동 삭제) | `{ "userId" }` |
| `GET` | `/api/chat-rooms/{id}/members` | 채팅방 멤버 조회 | - |

### 메시지 API

| Method | URL | 설명 | Request Body |
|--------|-----|------|-------------|
| `GET` | `/api/chat-rooms/{id}/messages?page=0&size=50` | 메시지 조회 (시간순, unreadCount 포함) | - |
| `POST` | `/api/messages/{id}/read` | 메시지 읽음 처리 (실시간 unreadCount 업데이트) | `{ "userId" }` |

### 서비스 정보

| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/service-info` | `{ "serviceName", "version", "status", "port" }` |
| `GET` | `/actuator/health` | 서비스 헬스 상태 (DB, Redis 포함) |

---

## WebSocket 엔드포인트

### 연결

- **엔드포인트:** `/ws` (SockJS)
- **프로토콜:** STOMP over SockJS

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    onConnect: () => console.log('Connected'),
});
client.activate();
```

### 메시지 전송 (Publish)

| Destination | 설명 | Payload |
|------------|------|---------|
| `/app/chat.send` | 채팅 메시지 전송 | `{ "roomId", "senderId", "content", "type": "CHAT" }` |
| `/app/chat.typing` | 타이핑 인디케이터 | `{ "roomId", "senderId" }` |

### 구독 (Subscribe)

| Destination | 설명 | 수신 데이터 |
|------------|------|------------|
| `/topic/chat-room/{roomId}` | 채팅방 메시지 수신 | `ChatMessageResponse` (unreadCount 포함) |
| `/topic/chat-room/{roomId}/read` | 읽음 업데이트 수신 | `{ "messageId", "unreadCount" }` |
| `/user/{userId}/queue/notifications` | 개인 알림 수신 | `Notification` |

---

## 마이크로서비스 통합 가이드

이 채팅 서비스는 독립적인 마이크로서비스로 설계되어, 향후 Master 웹서비스와 통합할 수 있습니다.

### 서비스 정보 엔드포인트

```
GET /api/service-info

{
    "serviceName": "tj-chat-service",
    "version": "1.0.0",
    "status": "running",
    "port": 8080
}
```

### Docker 네트워크 통합

Master 서비스와 같은 Docker 네트워크에 연결하여 내부 통신을 구성합니다:

```yaml
# Master 서비스의 docker-compose.yml에 추가
services:
  master-backend:
    networks:
      - master-network
      - chat-network

networks:
  chat-network:
    external: true
    name: tj-chat-service_chat-network
```

### API Gateway 라우팅 예시

```
/chat/api/** → http://chat-backend:8080/api/**
/chat/ws/**  → ws://chat-backend:8080/ws/**
```

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_HOST` | `localhost` | PostgreSQL 호스트 |
| `DB_PORT` | `5432` | PostgreSQL 포트 |
| `DB_NAME` | `chatdb` | 데이터베이스 이름 |
| `DB_USERNAME` | `chat` | 데이터베이스 사용자 |
| `DB_PASSWORD` | `chat1234` | 데이터베이스 비밀번호 |
| `REDIS_HOST` | `localhost` | Redis 호스트 |
| `REDIS_PORT` | `6379` | Redis 포트 |

---

## 외부 포트 매핑 (docker-compose.yml)

| 서비스 | 외부 포트 | 내부 포트 | 비고 |
|--------|----------|----------|------|
| PostgreSQL | 5433 | 5432 | tj-blog(5432)과 충돌 방지 |
| Redis | 6380 | 6379 | tj-blog과 동시 실행 가능 |
| Backend | 8081 | 8080 | tj-blog(8080)과 충돌 방지 |
| Frontend | 3000 | 80 | tj-blog(80)과 충돌 방지 |

> 단독 실행 시 `docker-compose.yml`에서 기본 포트(5432, 6379, 8080, 80)로 변경 가능합니다.
