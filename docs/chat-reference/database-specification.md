# Database Specification - TJ Chat Service

## 1. 접속 정보

### Docker Compose 환경 (기본)

| 항목 | 값 |
|------|-----|
| Host | `localhost` |
| Port | `5433` (외부) / `5432` (컨테이너 내부) |
| Database | `chatdb` |
| Username | `chat` |
| Password | `chat1234` |
| Container Name | `chat-db` |

> 포트 `5433`은 tj-blog(5432)과의 충돌을 피하기 위한 설정입니다.

### 접속 방법

#### CLI (psql)

```bash
# Docker 컨테이너에 직접 접속
docker exec -it chat-db psql -U chat -d chatdb

# 호스트에서 접속 (psql 설치 필요)
psql -h localhost -p 5433 -U chat -d chatdb
```

#### GUI 도구

**DBeaver / DataGrip / pgAdmin 등에서 아래 정보로 연결:**

| 설정 | 값 |
|------|-----|
| Connection Type | PostgreSQL |
| Host | `localhost` |
| Port | `5433` |
| Database | `chatdb` |
| User | `chat` |
| Password | `chat1234` |

**JDBC URL:**
```
jdbc:postgresql://localhost:5433/chatdb
```

#### IntelliJ IDEA Database 탭

1. `View` → `Tool Windows` → `Database`
2. `+` → `Data Source` → `PostgreSQL`
3. 위 접속 정보 입력 → `Test Connection` → `OK`

#### VS Code (SQLTools 확장)

1. SQLTools 확장 설치
2. `Add New Connection` → PostgreSQL 선택
3. 위 접속 정보 입력

---

## 2. ERD (Entity Relationship Diagram)

```
┌──────────────────┐       ┌──────────────────────┐       ┌──────────────────┐
│      users       │       │   chat_room_members   │       │    chat_rooms    │
├──────────────────┤       ├──────────────────────┤       ├──────────────────┤
│ PK id            │──┐    │ PK id                │    ┌──│ PK id            │
│    username (UQ) │  │    │ FK chat_room_id  ────│────┘  │    name          │
│    nickname      │  └────│ FK user_id       ────│       │    type          │
│    password      │       │    joined_at         │       │    created_at    │
│    status        │       └──────────────────────┘       └──────────────────┘
│    created_at    │                                              │
└──────────────────┘                                              │
        │                                                         │
        │         ┌──────────────────┐       ┌──────────────────┐ │
        │         │   read_receipts  │       │  chat_messages   │ │
        │         ├──────────────────┤       ├──────────────────┤ │
        │         │ PK id            │       │ PK id            │ │
        ├─────────│ FK user_id  ─────│       │ FK chat_room_id ─│─┘
        │         │ FK message_id ───│───────│ FK sender_id ────│──── users.id
        │         │    read_at       │       │    content       │
        │         └──────────────────┘       │    type          │
        │                                    │    created_at    │
        └────────────────────────────────────┘──────────────────┘
```

---

## 3. 테이블 명세

### 3.1 users (사용자)

| 컬럼 | 타입 | 제약조건 | 기본값 | 설명 |
|------|------|---------|--------|------|
| `id` | `BIGSERIAL` | PK, NOT NULL | auto_increment | 사용자 고유 ID |
| `username` | `VARCHAR(255)` | NOT NULL, UNIQUE | - | 로그인 아이디 |
| `nickname` | `VARCHAR(255)` | NOT NULL | - | 표시 이름 |
| `password` | `VARCHAR(255)` | NOT NULL | - | 비밀번호 (평문) |
| `status` | `VARCHAR(255)` | NOT NULL, CHECK | `'OFFLINE'` | 접속 상태 |
| `created_at` | `TIMESTAMP` | NOT NULL | `NOW()` | 가입일시 |

**status ENUM 값:** `ONLINE`, `OFFLINE`

**인덱스:**
- `UK_username` — username UNIQUE

---

### 3.2 chat_rooms (채팅방)

| 컬럼 | 타입 | 제약조건 | 기본값 | 설명 |
|------|------|---------|--------|------|
| `id` | `BIGSERIAL` | PK, NOT NULL | auto_increment | 채팅방 고유 ID |
| `name` | `VARCHAR(255)` | NOT NULL | - | 채팅방 이름 |
| `type` | `VARCHAR(255)` | NOT NULL, CHECK | `'GROUP'` | 채팅방 유형 |
| `created_at` | `TIMESTAMP` | NOT NULL | `NOW()` | 생성일시 |

**type ENUM 값:** `PRIVATE`, `DIRECT`, `GROUP`

- `DIRECT` — 1:1 채팅
- `GROUP` — 그룹 채팅
- `PRIVATE` — 비공개 채팅 (예약)

---

### 3.3 chat_room_members (채팅방 멤버)

| 컬럼 | 타입 | 제약조건 | 기본값 | 설명 |
|------|------|---------|--------|------|
| `id` | `BIGSERIAL` | PK, NOT NULL | auto_increment | 멤버 고유 ID |
| `chat_room_id` | `BIGINT` | FK, NOT NULL | - | 채팅방 ID |
| `user_id` | `BIGINT` | FK, NOT NULL | - | 사용자 ID |
| `joined_at` | `TIMESTAMP` | NOT NULL | `NOW()` | 참가일시 |

**외래키:**
- `chat_room_id` → `chat_rooms.id` (CASCADE DELETE)
- `user_id` → `users.id`

**비즈니스 규칙:**
- 같은 사용자가 같은 채팅방에 중복 참가 불가 (Service 레벨에서 체크)
- 멤버가 0명이 되면 채팅방 자동 삭제

---

### 3.4 chat_messages (채팅 메시지)

| 컬럼 | 타입 | 제약조건 | 기본값 | 설명 |
|------|------|---------|--------|------|
| `id` | `BIGSERIAL` | PK, NOT NULL | auto_increment | 메시지 고유 ID |
| `chat_room_id` | `BIGINT` | FK, NOT NULL | - | 채팅방 ID |
| `sender_id` | `BIGINT` | FK, NOT NULL | - | 발신자 ID |
| `content` | `TEXT` | NOT NULL | - | 메시지 내용 |
| `type` | `VARCHAR(255)` | NOT NULL, CHECK | `'TEXT'` | 메시지 유형 |
| `created_at` | `TIMESTAMP` | NOT NULL | `NOW()` | 전송일시 |

**type ENUM 값:** `TEXT`, `CHAT`, `ENTER`, `LEAVE`

- `CHAT` — 일반 채팅 메시지 (프론트엔드 기본값)
- `TEXT` — 텍스트 메시지 (백엔드 기본값)
- `ENTER` — 입장 메시지
- `LEAVE` — 퇴장 메시지

**외래키:**
- `chat_room_id` → `chat_rooms.id`
- `sender_id` → `users.id`

---

### 3.5 read_receipts (읽음 확인)

| 컬럼 | 타입 | 제약조건 | 기본값 | 설명 |
|------|------|---------|--------|------|
| `id` | `BIGSERIAL` | PK, NOT NULL | auto_increment | 읽음 고유 ID |
| `message_id` | `BIGINT` | FK, NOT NULL | - | 메시지 ID |
| `user_id` | `BIGINT` | FK, NOT NULL | - | 읽은 사용자 ID |
| `read_at` | `TIMESTAMP` | NOT NULL | `NOW()` | 읽은 시각 |

**외래키:**
- `message_id` → `chat_messages.id`
- `user_id` → `users.id`

**비즈니스 규칙:**
- 같은 사용자가 같은 메시지에 중복 읽음 처리 불가 (Service 레벨에서 체크)
- 메시지 발신자는 전송 시 자동으로 읽음 처리됨
- `unreadCount = 채팅방 멤버 수 - 해당 메시지의 read_receipts 수`

---

## 4. 주요 쿼리 패턴

### 메시지 조회 (시간순 + 안 읽은 수)

```sql
-- 채팅방의 메시지를 시간순으로 조회
SELECT m.*, u.nickname as sender_nickname
FROM chat_messages m
JOIN users u ON m.sender_id = u.id
WHERE m.chat_room_id = :roomId
ORDER BY m.created_at ASC
LIMIT :size OFFSET :page * :size;

-- 메시지별 안 읽은 수 계산
SELECT
    (SELECT COUNT(*) FROM chat_room_members WHERE chat_room_id = m.chat_room_id)
    - (SELECT COUNT(*) FROM read_receipts WHERE message_id = m.id)
    AS unread_count
FROM chat_messages m
WHERE m.id = :messageId;
```

### 채팅방 멤버 확인

```sql
-- 특정 채팅방의 멤버 목록
SELECT u.* FROM users u
JOIN chat_room_members crm ON u.id = crm.user_id
WHERE crm.chat_room_id = :roomId;

-- 멤버 수 카운트
SELECT COUNT(*) FROM chat_room_members WHERE chat_room_id = :roomId;
```

---

## 5. DDL 생성 방식

테이블은 **Hibernate `ddl-auto: update`** 설정에 의해 애플리케이션 시작 시 자동 생성됩니다.
별도의 `schema.sql` 파일은 사용하지 않습니다.

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 테이블 없으면 생성, 있으면 변경 감지
```

> **주의:** 운영 환경에서는 `ddl-auto: validate` 또는 `none`으로 변경하고 Flyway/Liquibase 등의 마이그레이션 도구를 사용해야 합니다.

---

## 6. 데이터 초기화

```bash
# DB 데이터 완전 초기화 (볼륨 삭제)
docker compose down -v
docker compose up -d --build

# 특정 테이블 데이터만 삭제 (psql)
docker exec -it chat-db psql -U chat -d chatdb -c "TRUNCATE read_receipts, chat_messages, chat_room_members, chat_rooms, users RESTART IDENTITY CASCADE;"
```

---

## 7. Redis 접속 정보

| 항목 | 값 |
|------|-----|
| Host | `localhost` |
| Port | `6380` (외부) / `6379` (내부) |
| Container Name | `chat-redis` |
| Pub/Sub Channel | `chat` |

```bash
# Redis CLI 접속
docker exec -it chat-redis redis-cli

# Pub/Sub 모니터링
docker exec -it chat-redis redis-cli SUBSCRIBE chat

# 전체 키 확인
docker exec -it chat-redis redis-cli KEYS '*'
```
