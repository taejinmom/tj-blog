# Phase 4: Event-Driven User Sync & Chat.users 축소

> **Status**: Draft
> **Author**: taejin
> **Created**: 2026-04-19
> **Depends on**: Phase 2 (Auth) · Phase 3 (Frontend Integration) · Phase 3+Option A (auto-provisioning 임시 조치)

---

## Context

Phase 2 에서 auth-service 를 신설해 JWT 기반 인증으로 통합했고, Phase 3 에서 프론트엔드에 로그인/채팅 UI 를 통합했다. 그러나 auth-service 와 chat-service 가 **각자 독립적인 `users` 테이블** 을 가지고 있으며, 이 둘 사이에 **공식적인 동기화 경로가 없다**.

Phase 3 테스트 중 발견:

- `authdb.users` 에 회원가입이 되어도 `chatdb.users` 에는 행이 없어 `ChatRoomService.join` 등에서 `User not found` 500 이 터졌다.
- 임시로 **Option A (JWT 필터에서 자동 프로비저닝)** 을 넣어 "chatdb 에 없는 userId 가 오면 즉석 insert" 로 막아 놓았다. 실제 체감되는 증상은 해소되었지만:
  - `displayName` 이 JWT 클레임에 없어 현재 `chat.users.nickname` 은 email 로컬파트("alice@foo.com" → "alice") 로 대체 저장됨. 유저가 `displayName` 을 바꿔도 chat 에는 영영 반영되지 않음.
  - 동일 정보가 두 DB 에 중복 저장되고, 둘 사이에 불일치 상태가 "정상"으로 취급됨.
  - 닉네임, 프로필 이미지 등 **유저가 수정 가능한 속성** 을 제대로 반영하려면 동기화 또는 단일 source-of-truth 가 필수.

Option A 는 **"동작은 하지만 닉네임 신선도(freshness) 가 없고 단일 SoT 원칙 위반"** 인 임시 조치다. Phase 4 에서 정공법으로 재설계한다.

---

## Goals

- auth-service 를 **유저 도메인의 단일 source-of-truth** 로 확립한다.
- chat-service 는 `users` 대신 **read-only projection (users_view)** 만 유지하고, 이것이 auth 의 사용자 변경 이벤트를 구독해 최종적으로 일치하도록 한다.
- `displayName` 변경이 수 초 내에 chat UI (senderNickname, 멤버 목록) 에 반영되도록 한다.
- 서비스 간 **직접 API 호출 없이** 메시지 버스 (Kafka 또는 Redis Stream) 를 통한 느슨한 결합을 유지한다.
- Option A (JWT 자동 프로비저닝) 를 제거한다.

## Non-goals

- auth 서비스의 프로필/아바타/상태메시지 기능 확장 (별도 Phase 에서)
- 유저 삭제/탈퇴 플로우 (이벤트 스펙만 정의, 실제 삭제 경로는 별도 Phase)
- 다른 서비스(블로그 등) 로의 동일 패턴 확장 — 필요 시 이 PRD 의 패턴을 재사용하되 스코프 밖
- CDC(Debezium) 기반 동기화 — 이 프로젝트 규모에는 과잉

---

## User Stories

### US-1: 닉네임 변경 반영
```
AS A 가입된 사용자
I WANT TO 프로필에서 displayName 을 바꾸면 채팅에서도 새 이름으로 보이게 하고 싶다
SO THAT 내 아이덴티티가 서비스 전체에서 일관되게 보인다
```
**AC**: auth 에서 `PATCH /api/users/me` 로 displayName 변경 → 10초 이내 chat 의 `senderNickname`, 멤버 패널, 방 목록 닉네임에 반영.

### US-2: 회원가입 후 즉시 채팅 사용
```
AS A 신규 사용자
I WANT TO 가입 직후 바로 채팅에 들어갈 수 있다
SO THAT "User not found" 같은 에러를 보지 않는다
```
**AC**: `/signup` 직후 `/chat` 접근 시 에러 없음 (수동 INSERT, Option A 자동 프로비저닝 없이 동작).

### US-3: chat-service 재시작 시 자동 재구축
```
AS A 운영자
I WANT TO chat-service 가 내려갔다 올라와도 놓친 이벤트를 따라잡기를 원한다
SO THAT 유저 데이터가 누락되지 않는다
```
**AC**: chat-service 재시작 후 consumer offset 부터 재생, 최신 상태로 수렴.

---

## Technical Design

### 아키텍처 개요

```
┌─────────────────────────┐       ┌──────────────────────┐
│   auth-service          │       │   chat-service       │
│                         │       │                      │
│  authdb.users (SoT)     │       │  chatdb.users_view   │
│  ├─ id, email,          │       │  ├─ id (same id)     │
│  │  display_name, ...   │       │  ├─ email            │
│  └─ outbox_events ──────┼──┐    │  └─ display_name     │
│                         │  │    │                      │
│  Publisher              │  │    │  Consumer ───────────┼──→ projection update
└─────────────────────────┘  │    └──────────▲───────────┘
                             │               │
                             ▼               │
                   ┌─────────────────────────┴─┐
                   │   Redis Stream            │
                   │   (stream: user.events)   │
                   └───────────────────────────┘
```

### 메시지 버스 선택: Redis Stream

| 옵션 | 선택 이유 | 비선택 이유 |
|------|----------|-------------|
| **Redis Stream (선택)** | 이미 `platform-redis` 가 docker-compose 에 있음. Consumer group, offset, persistence 모두 지원. 학습 곡선 낮음. | 고부하 환경에선 Kafka 가 우세 |
| Kafka | 업계 표준, 강력한 생태계 | Zookeeper/KRaft 추가 컨테이너, 프로젝트 규모 대비 오버엔지니어링 |
| RabbitMQ | 직관적 topology | 이미 Redis 존재 — 이걸 두고 새 브로커 추가할 이유 없음 |
| 직접 API 호출 (Option C 단독) | 가장 단순 | chat 이 auth 에 강하게 결합, auth 다운 시 chat 장애 |

**결정**: Redis Stream. 이미 배포된 인프라를 재활용하고, 코드량이 적으며, 데모 규모에 적합.

### 이벤트 스펙

**Stream name**: `user.events`

**Event types** (field `type` 에 문자열로):
| type | 발행 시점 | payload 필드 |
|------|----------|-------------|
| `user.created` | auth-service 에서 signup 성공 후 (로컬 or OAuth 둘 다) | `id`, `email`, `displayName`, `createdAt` |
| `user.updated` | displayName/email 변경 후 | `id`, `email`, `displayName`, `updatedAt` |
| `user.deleted` | (미래) 유저 삭제 시 | `id`, `deletedAt` |

**Envelope**:
```json
{
  "type": "user.created",
  "eventId": "uuid-v4",
  "occurredAt": "2026-04-19T12:34:56Z",
  "version": 1,
  "data": {
    "id": 42,
    "email": "alice@test.com",
    "displayName": "Alice Kim",
    "createdAt": "2026-04-19T12:34:55Z"
  }
}
```

- `eventId` 는 idempotency 키. consumer 가 중복 수신해도 같은 결과.
- `version` 은 스키마 버전. 호환 깨지는 변경은 +1 하고 consumer 가 구식 버전은 디폴트 처리.

### Task 1: auth-service — Outbox + Publisher

**신규 테이블**: `authdb.outbox_events`
```sql
CREATE TABLE outbox_events (
  id BIGSERIAL PRIMARY KEY,
  event_id UUID NOT NULL UNIQUE,
  stream_name VARCHAR(100) NOT NULL,
  type VARCHAR(100) NOT NULL,
  payload JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  published_at TIMESTAMP,
  INDEX (published_at)
);
```

**Outbox 패턴**:
1. `AuthService.signup()` / `updateProfile()` 트랜잭션 안에서 `user` 테이블 변경과 동시에 `outbox_events` 에 이벤트 insert.
2. 별도 스케줄러(`@Scheduled(fixedDelay = 1000)`) 가 `published_at IS NULL` 인 행을 읽어 Redis Stream 에 `XADD` 후 `published_at = NOW()` 업데이트.
3. 크래시/재시작 시 published_at 이 비어있는 행은 자동으로 재시도.

**장점**: DB 트랜잭션과 이벤트 발행이 **원자적** (트랜잭션 커밋 없이는 outbox 에도 없음).

**신규 파일**:
- `UserEventPublisher.java` — 스케줄러
- `OutboxEvent.java`, `OutboxEventRepository.java`
- `UserEventEnvelope.java` (record DTO)
- `RedisConfig.java` — RedisTemplate 빈
- `AuthService` 에 `outboxRepository.save(...)` 추가 (signup, OAuth provision, updateProfile)

**환경변수**:
- `REDIS_HOST`, `REDIS_PORT` (auth 서비스에도 추가 — 현재는 chat 만 접속)
- `USER_EVENTS_STREAM` (기본 `user.events`)

### Task 2: chat-service — Consumer + users_view

**스키마 변경**:
```sql
-- 기존 users 테이블 삭제
DROP TABLE users CASCADE;  -- ※ 먼저 의존 FK 재설계 필요

-- 새 users_view (read model)
CREATE TABLE users_view (
  id BIGINT PRIMARY KEY,  -- auth 의 user id 와 동일
  email VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

-- FK 는 유지하되 users_view 를 참조
ALTER TABLE chat_messages
  DROP CONSTRAINT fkgiqeap8ays4lf684x7m0r2729,
  ADD CONSTRAINT fk_chat_messages_sender
  FOREIGN KEY (sender_id) REFERENCES users_view(id) ON DELETE RESTRICT;
-- (chat_room_members, read_receipts 도 동일 처리)
```

- `users_view` 는 chat 이 **읽기만 하는** 테이블. INSERT/UPDATE 는 consumer 만 수행.
- `ON DELETE RESTRICT` 로 "메시지가 남아있으면 삭제 차단" 정책. (Phase 4 에서는 실제 삭제 플로우를 다루지 않으므로 문제 없음)

**신규 파일**:
- `UserEventConsumer.java` — Redis Stream consumer group 으로 `user.events` 구독
- `UserProjectionService.java` — `user.created` → `users_view` INSERT, `user.updated` → UPDATE
- `ProcessedEventRepository.java` — `event_id` 를 저장해 멱등성 확보

**기존 파일 수정**:
- `User.java` entity → `UserView.java` 로 개명 + 컬럼 구조 변경
- `UserRepository.java` → `UserViewRepository.java`
- `ChatMessage`, `ChatRoomMember`, `ReadReceipt` 의 `@ManyToOne User sender` → `UserView` 로 변경
- **`UserProvisioningService.java` 삭제** (Option A 제거)
- `JwtAuthenticationFilter.java` 에서 `userProvisioningService.provisionIfMissing()` 호출 제거
- `WebSocketAuthInterceptor.java` 에서도 동일 제거

**Consumer 동작**:
```java
@Component
@RequiredArgsConstructor
public class UserEventConsumer {
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final UserProjectionService projectionService;

    @PostConstruct
    void start() {
        container.receive(
            Consumer.from("chat-consumer-group", hostname),
            StreamOffset.create("user.events", ReadOffset.lastConsumed()),
            this::handle
        );
        container.start();
    }

    private void handle(MapRecord<String, String, String> rec) {
        UserEventEnvelope event = parse(rec);
        if (processedEventRepo.existsByEventId(event.eventId())) return;  // idempotent
        projectionService.apply(event);
        processedEventRepo.save(new ProcessedEvent(event.eventId()));
        container.acknowledge(rec);
    }
}
```

### Task 3: 마이그레이션 계획

**순서 중요** — 운영 중 데이터 유실을 막으려면:

1. **(선행) auth 서비스 outbox 테이블 추가 + publisher 배포**: 기존 유저 데이터는 스트림에 없지만, 이 시점 이후 모든 변경은 스트림에 쌓임.
2. **(선행) 기존 auth.users 전체를 `user.created` 이벤트로 backfill 발행**: 일회성 마이그레이션 잡. 이벤트 버전 1, `eventId` = `backfill-<userId>` 로 고정.
3. **chat-service 쪽 users_view 테이블 추가 + consumer 배포**: 2번에서 쌓인 backfill 이벤트를 소비해 테이블 초기화.
4. **기존 chat_messages/chat_room_members/read_receipts 의 sender_id/user_id 가 users_view.id 와 일치하는지 무결성 검증**.
5. **기존 `users` 테이블 DROP**: FK 재설계 완료 후.
6. **Option A 코드 제거**: `UserProvisioningService` 삭제, 필터에서 호출 제거.

### Task 4: 통합 테스트 시나리오

| # | 시나리오 | 기대 결과 |
|---|---------|----------|
| 1 | 회원가입 직후 `/chat` 접근 | User not found 없음 |
| 2 | auth 에서 displayName 변경 → 10초 후 chat 내 자기 메시지의 senderNickname 조회 | 새 이름 반영 |
| 3 | chat-service 다운 상태에서 auth 에 유저 5명 가입 → chat 재시작 | 5개 이벤트가 lag 없이 backfill, users_view 동기화 |
| 4 | Redis 다운 → 복구 후 outbox 에 쌓인 이벤트가 순차 발행됨 | 메시지 유실 없음 |
| 5 | 같은 eventId 를 consumer 가 2번 받음 (재전송 시뮬레이션) | 두 번째는 무시 (idempotency) |
| 6 | 잘못된 envelope (버전 > 1) 수신 | WARN 로그 + 무시, consumer 중단 없음 |

---

## Rollout

**Phase 4a — Publisher 준비** (인프라 변경 없이 auth 쪽 추가 컴포넌트만 배포)
- outbox 테이블 + publisher 배포 → 이벤트는 발행되지만 구독자 없음
- 기존 유저 backfill 발행

**Phase 4b — Consumer 배포 + read model 전환**
- chat-service 에 users_view + consumer 추가 (기존 `users` 는 남겨둠)
- 이중 쓰기 기간: 기존 `users` 유지하면서 `users_view` 도 채워짐
- 데이터 무결성 체크

**Phase 4c — cutover**
- chat FK 를 users_view 로 전환
- 기존 `users` 테이블 삭제
- Option A 코드 삭제

**롤백 포인트**:
- 4a 실패 시: outbox 스케줄러 disable 하면 원상 복구
- 4b 실패 시: consumer 중단, 기존 `users` 유지
- 4c 이후 롤백: 어렵다. 충분히 스테이징 테스트 후 진행

---

## Dependencies

**새 의존성 (auth-service)**:
- `spring-data-redis` (현재 auth 는 redis 미사용)
- `spring-boot-starter-data-redis`
- `spring-boot-starter-quartz` 또는 `@Scheduled` (이미 Spring Boot 에 내장)

**새 환경변수**:
- auth-service: `REDIS_HOST`, `REDIS_PORT`, `USER_EVENTS_STREAM`
- chat-service: 기존 Redis 연결 재사용, `USER_EVENTS_STREAM`, `USER_EVENTS_CONSUMER_GROUP`

**docker-compose**:
- auth-backend 에 `depends_on: platform-redis` 추가
- `REDIS_HOST=platform-redis` 환경변수 추가

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| 스트림 유실 (Redis 재시작, persistence 미설정) | 유저 변경 누락 → users_view 오래된 상태 유지 | Redis AOF persistence 활성화, outbox 패턴으로 재발행 가능 |
| Consumer 처리 지연 → UI 에서 "옛 닉네임" 노출 | 사용자 혼란 | P99 처리 지연 SLO 10초 설정 + Grafana 알람 |
| 이벤트 순서 뒤집힘 (updated 가 created 보다 먼저) | users_view 에 INSERT 실패 | consumer 에서 `created` 누락 시 upsert(`ON CONFLICT DO UPDATE`) 로 관용 처리 |
| backfill 중복 발행 | 동일 이벤트 2회 수신 | `eventId` 기반 `processed_events` 테이블 체크 (idempotency) |
| 스키마 마이그레이션 중 FK 깨짐 | 채팅 메시지 조회 실패 | 마이그레이션 전 full backup + 이중 쓰기 기간 |
| 운영자가 outbox 직접 조작 | 이벤트 누락/중복 | outbox 는 append-only 문서화, 접근 제한 |

---

## Open Questions

- `user.deleted` 이벤트의 의미: chat 에서 실제 row 삭제 vs soft-delete (tombstone)? 프로필 사진 삭제 등 연관 리소스 처리 정책 필요. → **이번 Phase 에서는 이벤트 스펙만 정의, 실제 삭제 consumer 는 미구현**.
- JWT 클레임에 displayName 을 넣을지 여부: 넣으면 네트워크 호출 없이도 "현재 사용자 이름" 을 바로 알 수 있음. 단점은 변경 시 새 토큰 발급 전까지 stale. → **별개의 트레이드오프, 이 PRD 스코프 밖**.
- blog-service 도 동일 패턴이 필요한가? 현재 blog 는 author 를 단순 문자열 or 고정값으로 저장 중이라 필요 없어 보임. → 추후 블로그에 "작성자 프로필 링크" 같은 기능이 생길 때 재검토.

---

## References

- Outbox Pattern: https://microservices.io/patterns/data/transactional-outbox.html
- Redis Streams: https://redis.io/docs/latest/develop/data-types/streams/
- Event-Driven Architecture (Chris Richardson): chapter on data consistency
- CAP Theorem trade-offs in microservice data: 이 디자인은 AP (availability + partition tolerance) 우선, 최종 일관성 수용
