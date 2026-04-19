# Developer Source Review Guide - TJ Chat Service

이 문서는 개발자가 소스 코드를 워크플로우 순서대로 따라가며 이해할 수 있도록 작성되었습니다.
각 기능별로 **사용자 액션 → Frontend → Backend → DB/Redis** 흐름을 따라갑니다.

---

## 목차

1. [프로젝트 진입점](#1-프로젝트-진입점)
2. [회원가입 / 로그인 플로우](#2-회원가입--로그인-플로우)
3. [채팅방 생성 플로우](#3-채팅방-생성-플로우)
4. [채팅방 입장 플로우](#4-채팅방-입장-플로우)
5. [WebSocket 연결 플로우](#5-websocket-연결-플로우)
6. [메시지 전송 플로우 (핵심)](#6-메시지-전송-플로우-핵심)
7. [메시지 수신 플로우 (핵심)](#7-메시지-수신-플로우-핵심)
8. [읽음 표시 플로우](#8-읽음-표시-플로우)
9. [채팅방 나가기 플로우](#9-채팅방-나가기-플로우)
10. [설정 및 인프라](#10-설정-및-인프라)

---

## 1. 프로젝트 진입점

### Backend

```
backend/src/main/java/com/taejin/chat/ChatApplication.java
```

Spring Boot 메인 클래스. `@SpringBootApplication`으로 컴포넌트 스캔 시작점.

**설정 파일:**
```
backend/src/main/resources/application.yml
```
- DB 연결 (PostgreSQL), Redis 연결, JPA 설정, Actuator 설정

### Frontend

```
frontend/src/main.tsx          → ReactDOM 렌더링 진입점
frontend/src/App.tsx           → 라우팅 정의
```

**라우팅 구조:**
```
/           → LoginPage.tsx        (로그인/회원가입)
/chat       → Layout.tsx           (사이드바 + Outlet)
/chat/:roomId → ChatRoom.tsx       (채팅 화면)
```

`Layout.tsx`가 WebSocket 연결을 관리하고, `Outlet context`로 하위 컴포넌트에 전달합니다.

---

## 2. 회원가입 / 로그인 플로우

### 시퀀스

```
사용자 입력 → LoginPage.tsx → api.ts → UserController → UserService → UserRepository → DB
```

### 소스 추적

#### 1단계: 사용자 입력 (Frontend)

```
frontend/src/components/LoginPage.tsx
```
- `handleSubmit()` → 회원가입: `userApi.register()`, 로그인: `userApi.login()`
- 성공 시 `localStorage`에 `userId`, `nickname`, `username` 저장
- `/chat`으로 네비게이트

#### 2단계: API 호출 (Frontend)

```
frontend/src/services/api.ts
```
- `userApi.register()` → `POST /api/users` body: `{ username, nickname, password }`
- `userApi.login()` → `POST /api/users/login` body: `{ username, password }`
- `api` 인스턴스에 `X-User-Id` 헤더 인터셉터 설정

#### 3단계: API 수신 (Backend)

```
backend/.../controller/UserController.java
```
- `@PostMapping` `/api/users` → `register()` → `userService.register()`
- `@PostMapping` `/api/users/login` → `login()` → `userService.login()`

#### 4단계: 비즈니스 로직 (Backend)

```
backend/.../service/UserService.java
```
- `register()`: username 중복 체크 → User 엔티티 생성 → `userRepository.save()`
- `login()`: username으로 조회 → 비밀번호 검증 → `status = ONLINE` 변경 → 저장

#### 5단계: 데이터 접근 (Backend)

```
backend/.../repository/UserRepository.java
```
- `findByUsername()`: 로그인 시 사용
- `existsByUsername()`: 가입 시 중복 체크

#### 관련 Entity & DTO

```
backend/.../entity/User.java           → DB 테이블 매핑 (users)
backend/.../dto/UserRequest.java        → 가입 요청 DTO
backend/.../dto/LoginRequest.java       → 로그인 요청 DTO
backend/.../dto/UserResponse.java       → 응답 DTO (id, username, nickname, status, createdAt)
```

---

## 3. 채팅방 생성 플로우

### 시퀀스

```
사용자 클릭 → CreateRoomModal.tsx → api.ts → ChatRoomController → ChatRoomService → DB
```

### 소스 추적

#### 1단계: 모달 UI (Frontend)

```
frontend/src/components/CreateRoomModal.tsx
```
- 채팅방 이름 입력, 타입 선택 (GROUP / DIRECT)
- `handleSubmit()` → `chatRoomApi.createRoom({ name, type, creatorId })`
- 성공 시 `onCreated()` 콜백 → 모달 닫기 + 목록 갱신

#### 2단계: API 호출 (Frontend)

```
frontend/src/services/api.ts
```
- `chatRoomApi.createRoom()` → `POST /api/chat-rooms`

#### 3단계: 채팅방 생성 (Backend)

```
backend/.../controller/ChatRoomController.java
  → @PostMapping → chatRoomService.create(request)

backend/.../service/ChatRoomService.java
  → create(): ChatRoom 엔티티 생성 → DB 저장 → 생성자를 멤버로 추가 → ChatRoomResponse 반환
```

**핵심 포인트:**
- 채팅방 생성 시 `creatorId`의 유저를 자동으로 첫 번째 멤버로 등록
- `room.getMembers().add(member)` — 응답의 `memberCount`에 반영

#### 4단계: 목록 갱신 (Frontend)

```
frontend/src/components/ChatRoomList.tsx
```
- `handleRoomCreated()` → `fetchRooms()` → 채팅방 목록 재조회
- 10초 간격 폴링으로도 목록 자동 갱신

---

## 4. 채팅방 입장 플로우

### 시퀀스

```
사용자 클릭 → ChatRoomList.tsx → navigate → ChatRoom.tsx → api.ts → ChatRoomController → DB
```

### 소스 추적

```
frontend/src/components/ChatRoomList.tsx
```
- 채팅방 클릭 → `navigate('/chat/{roomId}')`

```
frontend/src/components/ChatRoom.tsx
```
- `useEffect` (roomId 변경 시):
  1. `chatRoomApi.getRoom(roomId)` → 채팅방 정보 조회
  2. `chatRoomApi.joinRoom(roomId, userId)` → 멤버로 참가
  3. `.then()` → `chatRoomApi.getMembers(roomId)` → 멤버 목록 조회
- `useChat` 훅으로 메시지 구독 시작 (5번 참조)

```
backend/.../service/ChatRoomService.java
```
- `join()`: 이미 참가한 멤버인지 `existsByChatRoomIdAndUserId`로 확인 → 중복이면 무시

---

## 5. WebSocket 연결 플로우

### 시퀀스

```
Layout.tsx → useWebSocket → websocket.ts → SockJS → Nginx(/ws) → Backend(WebSocketConfig)
```

### 소스 추적

#### 1단계: 연결 시작 (Frontend)

```
frontend/src/components/Layout.tsx
```
- `useWebSocket(userId)` 훅 호출 → WebSocket 연결 + 알림 구독
- `connected`, `subscribeToRoom`, `sendMessage`를 `Outlet context`로 전달

```
frontend/src/hooks/useWebSocket.ts
```
- `useEffect`: `wsService.connect()` 호출
- 연결 성공 시: `setConnected(true)` + `/user/{userId}/queue/notifications` 구독
- `subscribeToRoom(roomId, callback)` → `/topic/chat-room/{roomId}` 구독
- `sendMessage(roomId, content, senderId)` → `/app/chat.send`로 STOMP publish

```
frontend/src/services/websocket.ts
```
- `WebSocketService` 클래스 (싱글톤 `wsService`)
- `connect()`: `new Client({ webSocketFactory: () => new SockJS('/ws') })`
- `subscribe()`: `client.subscribe(destination, callback)` — 이미 구독 중이면 무시
- `publish()`: `client.publish({ destination, body: JSON.stringify(body) })`
- `unsubscribe()`: 구독 해제 + subscriptions Map에서 제거

#### 2단계: 서버 수신 (Backend)

```
backend/.../config/WebSocketConfig.java
```
- `@EnableWebSocketMessageBroker`
- `registerStompEndpoints`: `/ws` 엔드포인트 등록 (SockJS 지원)
- `configureMessageBroker`:
  - SimpleBroker: `/topic`, `/queue` (서버 → 클라이언트 메시지 경로)
  - ApplicationDestinationPrefixes: `/app` (클라이언트 → 서버 메시지 경로)
  - UserDestinationPrefix: `/user`

```
backend/.../config/WebSocketEventListener.java
```
- `SessionConnectedEvent` → 연결 로그
- `SessionDisconnectEvent` → 해제 로그

#### 핵심 포인트: connected 상태와 구독 타이밍

```
frontend/src/hooks/useChat.ts
```
- `useEffect`의 의존성에 `connected` 포함
- WebSocket 연결 전(`connected=false`)에는 구독하지 않음
- 연결 완료(`connected=true`)되면 `useEffect` 재실행 → 구독 성공
- **이 타이밍 처리가 없으면 메시지가 실시간으로 표시되지 않음**

---

## 6. 메시지 전송 플로우 (핵심)

이 프로젝트의 가장 핵심적인 플로우입니다.

### 시퀀스

```
사용자 Enter → MessageInput → useChat.sendMessage → useWebSocket.sendMessage
  → wsService.publish('/app/chat.send') → [STOMP]
  → StompChatController.sendMessage → ChatMessageService.save → DB 저장
  → RedisPublisher.publish → [Redis Pub/Sub 'chat' 채널]
  → RedisSubscriber.onMessage → SimpMessagingTemplate.convertAndSend
  → [STOMP '/topic/chat-room/{roomId}']
  → 구독 중인 모든 클라이언트에 전달
```

### 소스 추적

#### 1단계: 입력 UI (Frontend)

```
frontend/src/components/MessageInput.tsx
```
- `handleKeyDown`: Enter → `handleSend()`, Shift+Enter → 줄바꿈
- `handleSend()` → `onSend(content)` 콜백

#### 2단계: 메시지 전송 (Frontend)

```
frontend/src/hooks/useChat.ts → sendMessage()
  → wsSendMessage(roomId, content, userId)

frontend/src/hooks/useWebSocket.ts → sendMessage()
  → wsService.publish('/app/chat.send', { roomId, senderId, content, type: 'CHAT' })

frontend/src/services/websocket.ts → publish()
  → client.publish({ destination, body: JSON.stringify(body) })
```

#### 3단계: STOMP 수신 (Backend)

```
backend/.../controller/StompChatController.java
```
- `@MessageMapping("/chat.send")` → `sendMessage(@Payload ChatMessageRequest request)`
- `messageService.save(request)` → DB 저장 + 발신자 ReadReceipt 생성
- `redisPublisher.publish(response)` → Redis로 발행

#### 4단계: DB 저장 + unreadCount 계산 (Backend)

```
backend/.../service/ChatMessageService.java → save()
```
1. `chatRoomRepository.findById(request.getRoomId())` → 채팅방 조회
2. `userRepository.findById(request.getSenderId())` → 발신자 조회
3. `ChatMessage` 엔티티 생성 → `messageRepository.save()`
4. 발신자의 `ReadReceipt` 자동 생성 → `readReceiptRepository.save()`
5. `unreadCount = memberCount - 1` (발신자만 읽은 상태)
6. `ChatMessageResponse.from(saved, unreadCount)` 반환

#### 5단계: Redis Pub/Sub 발행 (Backend)

```
backend/.../service/RedisPublisher.java
```
- `objectMapper.writeValueAsString(message)` → JSON 직렬화
- `redisTemplate.convertAndSend("chat", json)` → Redis 'chat' 채널로 발행

**핵심 포인트:**
- `RedisTemplate`의 value serializer는 `StringRedisSerializer`
- Publisher에서 직접 JSON 문자열을 만들어 전달
- 만약 `Jackson2JsonRedisSerializer`를 사용하면 **이중 직렬화** 문제 발생

#### 6단계: Redis 구독 + WebSocket 브로드캐스트 (Backend)

```
backend/.../service/RedisSubscriber.java
```
- `MessageListener` 인터페이스 구현
- `onMessage()`: Redis에서 받은 메시지 → JSON 역직렬화 → `ChatMessageResponse`
- `messagingTemplate.convertAndSend("/topic/chat-room/" + roomId, chatMessage)`
- → STOMP 구독자 전체에게 브로드캐스트

```
backend/.../config/RedisConfig.java
```
- `RedisMessageListenerContainer`에 `RedisSubscriber`를 `chat` 토픽에 등록
- **이 설정이 Redis ↔ WebSocket 브리지 역할**

---

## 7. 메시지 수신 플로우 (핵심)

### 시퀀스

```
[STOMP '/topic/chat-room/{roomId}'] → websocket.ts 구독 콜백
  → useWebSocket.subscribeToRoom 콜백 → useChat setMessages → MessageList → MessageItem
```

### 소스 추적

```
frontend/src/hooks/useChat.ts
```
- `subscribeToRoom(roomId, callback)` → 새 메시지 수신 시:
  - `setMessages(prev => [...prev, msg])` → 메시지 목록에 추가 (리렌더링)
  - 다른 사람의 메시지면 `messageApi.markAsRead()` 자동 호출

```
frontend/src/components/MessageList.tsx
```
- `messages` 배열을 순회 → 날짜 구분선 + `MessageItem` 렌더링
- `useEffect([messages])` → 새 메시지 시 자동 스크롤

```
frontend/src/components/MessageItem.tsx
```
- `isOwn` 여부에 따라 좌/우 배치 + 다른 스타일
- `unreadCount > 0`이면 노란색 숫자 표시 (카카오톡 스타일)
- `ENTER`/`LEAVE`/`SYSTEM` 타입은 중앙 시스템 메시지로 표시

---

## 8. 읽음 표시 플로우

### 시퀀스

```
메시지 수신 → useChat (자동 markAsRead) → api.ts → MessageController
  → ChatMessageService.markAsRead → ReadReceipt 저장 → DB
  → SimpMessagingTemplate.convertAndSend('/topic/chat-room/{roomId}/read')
  → 구독 중인 클라이언트 → useChat (unreadCount 업데이트) → MessageItem 리렌더링
```

### 소스 추적

#### 읽음 처리 요청 (Frontend)

```
frontend/src/hooks/useChat.ts
```
- 채팅방 입장 시: `markLastAsRead(messages)` → 마지막 메시지 읽음 처리
- 실시간 메시지 수신 시: `msg.senderId !== userId`면 자동 `markAsRead(msg.id, userId)`

```
frontend/src/services/api.ts
```
- `messageApi.markAsRead(messageId, userId)` → `POST /api/messages/{id}/read`

#### 읽음 처리 (Backend)

```
backend/.../controller/MessageController.java
  → @PostMapping("/api/messages/{id}/read") → messageService.markAsRead()

backend/.../service/ChatMessageService.java → markAsRead()
```
1. 이미 읽었으면 무시 (`existsByMessageIdAndUserId`)
2. `ReadReceipt` 생성 → DB 저장
3. `unreadCount` 재계산: `memberCount - readReceiptCount`
4. **WebSocket 브로드캐스트**: `/topic/chat-room/{roomId}/read` → `{ messageId, unreadCount }`

#### 읽음 업데이트 수신 (Frontend)

```
frontend/src/hooks/useChat.ts
```
- `/topic/chat-room/${roomId}/read` 구독
- `ReadUpdate` 수신 → `setMessages`에서 해당 메시지의 `unreadCount` 업데이트
- → `MessageItem` 리렌더링 → 숫자 감소 또는 사라짐

---

## 9. 채팅방 나가기 플로우

### 시퀀스

```
나가기 버튼 → confirm → api.ts → ChatRoomController → ChatRoomService.leave → DB
  → 멤버 0명이면 채팅방 삭제
```

### 소스 추적

```
frontend/src/components/ChatRoom.tsx
```
- 나가기 버튼 (문 아이콘) 클릭 → `confirm('채팅방을 나가시겠습니까?')`
- 확인 → `chatRoomApi.leaveRoom(roomId, userId).then(() => navigate('/chat'))`

```
frontend/src/services/api.ts
```
- `chatRoomApi.leaveRoom()` → `POST /api/chat-rooms/{id}/leave` body: `{ userId }`

```
backend/.../controller/ChatRoomController.java
  → @PostMapping("/{id}/leave") → chatRoomService.leave()

backend/.../service/ChatRoomService.java → leave()
```
1. `existsByChatRoomIdAndUserId` → 멤버가 아니면 무시
2. `memberRepository.deleteByChatRoomIdAndUserId()` → 멤버에서 제거
3. `countByChatRoomId == 0`이면 → `chatRoomRepository.deleteById()` → 채팅방 삭제

---

## 10. 설정 및 인프라

### Backend 설정 파일

| 파일 | 역할 |
|------|------|
| `config/WebSocketConfig.java` | STOMP 엔드포인트(`/ws`), 브로커(`/topic`, `/queue`), 앱 접두사(`/app`) |
| `config/RedisConfig.java` | `RedisTemplate`(StringSerializer), `RedisMessageListenerContainer`(chat 토픽) |
| `config/CorsConfig.java` | 모든 출처 허용 (`*`) |
| `config/WebSocketEventListener.java` | WebSocket 연결/해제 이벤트 로깅 |
| `controller/GlobalExceptionHandler.java` | `IllegalArgumentException` → 400, 기타 → 500 |
| `controller/ServiceInfoController.java` | `/api/service-info` — 마이크로서비스 메타데이터 |

### Frontend 설정 파일

| 파일 | 역할 |
|------|------|
| `vite.config.ts` | 개발 서버 포트(3000), `/api` `/ws` 프록시 설정 |
| `tailwind.config.js` | 다크 테마 커스텀 컬러 정의 |
| `nginx.conf` | 프로덕션 Nginx: SPA 라우팅, API/WebSocket 프록시 |

### Docker 구성

| 파일 | 역할 |
|------|------|
| `docker-compose.yml` | 4개 서비스 정의 (db, redis, backend, frontend) |
| `backend/Dockerfile` | Multi-stage build (Gradle → JRE 17) |
| `frontend/Dockerfile` | Multi-stage build (Node → Nginx) |

### 데이터 흐름 요약

```
┌──────────┐     STOMP      ┌───────────────┐     Redis     ┌───────────────┐
│ Frontend │ ──────────────▶ │ StompChat     │ ────────────▶ │ Redis         │
│          │                 │ Controller    │   Pub/Sub     │ (chat 채널)   │
│          │     REST API    │               │               │               │
│          │ ──────────────▶ │ User/Room/Msg │               └───────┬───────┘
│          │                 │ Controller    │                       │
│          │                 └───────┬───────┘                       │
│          │                         │                               │
│          │                         ▼                               ▼
│          │                 ┌───────────────┐               ┌───────────────┐
│          │                 │   Service     │               │ Redis         │
│          │                 │   Layer       │               │ Subscriber    │
│          │                 └───────┬───────┘               └───────┬───────┘
│          │                         │                               │
│          │                         ▼                               │
│          │                 ┌───────────────┐                       │
│          │                 │  PostgreSQL   │       STOMP           │
│          │ ◀───────────────│  (JPA)        │ ◀─────────────────────┘
│          │   STOMP 구독    └───────────────┘   convertAndSend
└──────────┘   (/topic/*)                        (/topic/chat-room/*)
```

---

## 부록: 소스 리뷰 체크리스트

코드 리뷰 시 아래 순서로 확인하면 전체 구조를 빠르게 파악할 수 있습니다.

### Phase 1: 전체 구조 파악 (5분)

- [ ] `App.tsx` → 라우팅 구조
- [ ] `Layout.tsx` → WebSocket 연결 시점, Outlet context
- [ ] `application.yml` → DB/Redis/Actuator 설정
- [ ] `docker-compose.yml` → 서비스 구성 및 의존성

### Phase 2: REST API 흐름 (10분)

- [ ] `api.ts` → API 엔드포인트 목록
- [ ] `UserController` → `UserService` → `UserRepository`
- [ ] `ChatRoomController` → `ChatRoomService` → `ChatRoomRepository`, `ChatRoomMemberRepository`
- [ ] `MessageController` → `ChatMessageService` → `ChatMessageRepository`, `ReadReceiptRepository`

### Phase 3: WebSocket 실시간 통신 (15분)

- [ ] `websocket.ts` → STOMP 클라이언트 싱글톤
- [ ] `useWebSocket.ts` → 연결 관리, 구독/발행
- [ ] `useChat.ts` → `connected` 의존성, 메시지 구독 + 읽음 구독
- [ ] `WebSocketConfig.java` → STOMP 설정
- [ ] `StompChatController.java` → 메시지 수신 → 저장 → Redis 발행

### Phase 4: Redis Pub/Sub 브리지 (5분)

- [ ] `RedisConfig.java` → Template(StringSerializer) + ListenerContainer
- [ ] `RedisPublisher.java` → JSON 직렬화 → `convertAndSend("chat", json)`
- [ ] `RedisSubscriber.java` → JSON 역직렬화 → `convertAndSend("/topic/...")`

### Phase 5: 읽음 표시 (5분)

- [ ] `ChatMessageService.save()` → 발신자 자동 ReadReceipt + unreadCount 계산
- [ ] `ChatMessageService.markAsRead()` → ReadReceipt 저장 → WebSocket 브로드캐스트
- [ ] `useChat.ts` → `/topic/chat-room/{id}/read` 구독 → unreadCount 업데이트
- [ ] `MessageItem.tsx` → `unreadCount > 0` 표시
