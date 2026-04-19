# auth-service 사용 가이드

브라우저·Swagger·H2 콘솔·curl 을 이용해 auth-service 를 직접 체험해보는 가이드입니다.
설치/빌드는 [README.md](./README.md) 의 Getting Started 를 먼저 참고하세요.

---

## 1. 서버 기동

```bash
cd auth-service
./gradlew bootRun
```

콘솔에 `Started AuthServiceApplication in X seconds` 가 뜨면 성공입니다. 종료는 `Ctrl+C`.

> ⚠️ 이 터미널 창은 서버가 돌아가는 동안 닫지 마세요. 다른 명령은 **새 터미널 창** 에서 실행합니다.
> WSL 에서 돌려도 Windows 브라우저에서 `http://localhost:8080` 으로 바로 접근됩니다 (WSL2 자동 포트 포워딩).

---

## 2. 브라우저에서 바로 확인할 수 있는 URL

| URL | 확인 내용 |
|------|-----------|
| <http://localhost:8080/actuator/health> | `{"status":"UP"}` — 헬스체크 |
| <http://localhost:8080/swagger-ui.html> | **Swagger UI — 모든 API 를 브라우저에서 직접 호출** |
| <http://localhost:8080/v3/api-docs> | OpenAPI 스펙 (JSON) |
| <http://localhost:8080/h2-console> | H2 인메모리 DB 콘솔 |

---

## 3. Swagger UI 로 전체 인증 플로우 테스트 (권장)

<http://localhost:8080/swagger-ui.html> 에 접속하면 `auth-controller`, `user-controller`, `admin-controller` 3개 그룹이 보입니다.

### 3-1. 회원가입
1. `POST /api/auth/signup` 펼치기 → **Try it out**
2. Request body 입력:
   ```json
   {
     "email": "test@example.com",
     "password": "pass1234",
     "displayName": "tester"
   }
   ```
3. **Execute** → `200 OK` 와 사용자 정보가 반환되면 성공.

### 3-2. 로그인 → 토큰 수신
1. `POST /api/auth/login` → **Try it out**
2. Request body:
   ```json
   { "email": "test@example.com", "password": "pass1234" }
   ```
3. **Execute** → 응답 JSON 에서 `accessToken`, `refreshToken` 두 값을 각각 복사해 두세요.
   - access 토큰 유효기간: **15분**
   - refresh 토큰 유효기간: **14일**

### 3-3. 인증이 필요한 API 호출 — `Authorize` 버튼
1. Swagger UI 오른쪽 상단 **🔒 Authorize** 버튼 클릭.
2. `Value` 입력창에 access 토큰을 붙여넣기.
   - 기본은 `Bearer ` 프리픽스가 자동 부착됩니다. 그래도 401 이 나면 `Bearer <token>` 형식으로 전체 입력해 보세요.
3. **Authorize** → **Close**.
4. `GET /api/users/me` → **Try it out** → **Execute**.
   - `200 OK` 와 본인 정보가 뜨면 JWT 인증 성공.

### 3-4. 관리자 권한 차단 확인 (실패 케이스)
1. `GET /api/admin/users` 실행.
2. `403 Forbidden` → **정상**. 현재 계정은 `ROLE_USER` 뿐이라 접근이 막힙니다.
3. 관리자 접근을 테스트하려면 [5장 H2 콘솔](#5-h2-콘솔로-db-확인--권한-부여) 에서 `ROLE_ADMIN` 을 수동 부여한 뒤 **재로그인**해서 새 토큰을 받으세요.
   - 기존 토큰에는 권한 클레임이 과거 스냅샷이라 그대로 쓸 수 없습니다.

### 3-5. 토큰 갱신 (refresh rotation)
1. `POST /api/auth/refresh` → body:
   ```json
   { "refreshToken": "복사한_refresh_토큰" }
   ```
2. **Execute** → 새 access/refresh 토큰 수신.
3. 기존 refresh 토큰은 즉시 `revoked=true` 처리되어 재사용 시 `401` 이 납니다 (rotation 방식).

### 3-6. 로그아웃
1. `POST /api/auth/logout` → body 에 현재 refresh 토큰을 넣고 **Execute**.
2. 해당 refresh 토큰만 폐기됩니다. access 토큰은 만료될 때까지는 서버 측 블랙리스트가 없으므로 주의.

---

## 4. OAuth2 소셜 로그인 테스트 (선택)

환경변수가 비어 있으면 registration 이 로드되지 않아 비활성 상태입니다. 테스트하려면 서버 기동 **전에**:

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export GITHUB_CLIENT_ID=...
export GITHUB_CLIENT_SECRET=...
./gradlew bootRun
```

그 후 브라우저로 직접 접근:
- <http://localhost:8080/api/auth/oauth2/authorize/google>
- <http://localhost:8080/api/auth/oauth2/authorize/github>

제공자 인증 완료 시 **콜백 엔드포인트가 JSON 으로 자체 JWT 를 반환** 합니다 (SPA 친화적 구조).

### Provider 콘솔 설정
- Google Cloud Console → APIs & Services → Credentials → OAuth client ID (Web)
  - Redirect URI: `http://localhost:8080/api/auth/oauth2/callback/google`
- GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
  - Homepage URL: `http://localhost:8080`
  - Callback URL: `http://localhost:8080/api/auth/oauth2/callback/github`

### 빠른 동작 확인 (자격증명 없이도 가능)
- 미등록 provider 접근: <http://localhost:8080/api/auth/oauth2/authorize/unknown> → **404** 가 정상 (`LenientOAuth2AuthorizationRequestResolver` 가 예외 대신 null 을 반환하도록 커스터마이즈됨).

---

## 5. H2 콘솔로 DB 확인 / 권한 부여

<http://localhost:8080/h2-console> 접속 후:

| 필드 | 값 |
|------|----|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:authdb` |
| User Name | `sa` |
| Password | (비움) |

**Connect** 를 누르면 좌측에 `USERS`, `ROLES`, `USER_ROLES`, `REFRESH_TOKENS`, `USER_IDENTITIES` 테이블이 보입니다.

### 유용한 쿼리

```sql
-- 가입된 사용자 목록
SELECT id, email, enabled, created_at FROM users;

-- 특정 사용자에게 ROLE_ADMIN 부여 (관리자 API 테스트용)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'test@example.com' AND r.name = 'ROLE_ADMIN';

-- 권한 부여 후에는 반드시 재로그인 — JWT roles 클레임은 로그인 시점 스냅샷

-- refresh 토큰 상태 확인
SELECT id, user_id, revoked, expires_at, created_at
FROM refresh_tokens
ORDER BY id DESC;

-- OAuth 연결된 외부 identity 확인
SELECT provider, provider_user_id, user_id, email_at_provider, created_at
FROM user_identities;
```

> 💡 H2 는 인메모리라 서버 재시작 시 데이터가 모두 사라집니다. 테스트 시나리오를 기록해두면 재현에 편합니다.

---

## 6. curl 로 대안 테스트 (새 터미널)

Swagger 가 번거롭다면 curl 로도 동일한 플로우를 돌릴 수 있습니다.

```bash
# 1. 회원가입
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass1234","displayName":"tester"}'

# 2. 로그인 → 토큰을 환경변수에 저장
TOKENS=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass1234"}')
echo "$TOKENS"

ACCESS=$(echo "$TOKENS"  | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
REFRESH=$(echo "$TOKENS" | python3 -c "import sys,json;print(json.load(sys.stdin)['refreshToken'])")

# 3. 보호된 API 호출
curl -s http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $ACCESS"

# 4. 관리자 API (현재는 403 이 정상)
curl -si http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ACCESS" | head -1

# 5. 토큰 갱신
curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# 6. 로그아웃
curl -s -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

---

## 7. 자주 마주치는 이슈

| 증상 | 원인 / 해결 |
|------|-------------|
| `localhost:8080` 접속 불가 | 서버 터미널에서 `Started AuthServiceApplication` 확인. 방화벽/프록시 점검 |
| `GET /api/users/me` 가 `401` | Swagger Authorize 안 했거나 access 토큰이 만료(15분). 재로그인 또는 refresh |
| `GET /api/admin/users` 가 `403` | 정상. 5장을 참고해 `ROLE_ADMIN` 부여 후 **재로그인** |
| refresh 후 기존 refresh 재사용 시 `401` | 정상 — rotation 으로 폐기된 토큰. 항상 최신 refresh 토큰만 보관하세요 |
| H2 콘솔 접속이 화면 공백 | 브라우저 주소가 `/h2-console` 인지 확인. JDBC URL 은 `jdbc:h2:mem:authdb` 정확히 입력 |
| Swagger Authorize 해도 401 | `Bearer <token>` 형태로 전체 입력해 보기. 또는 curl 로 헤더 직접 설정해 재현 |
| OAuth `redirect_uri_mismatch` | 제공자 콘솔의 콜백 URL 이 `http://localhost:8080/api/auth/oauth2/callback/{provider}` 와 **정확히** 일치해야 함 |
| 재시작하니 가입한 계정이 사라짐 | H2 인메모리라서 정상. 영속 저장이 필요하면 postgres 프로파일로 전환 (README DB 프로파일 섹션 참고) |

---

## 8. 추천 시나리오: 종단 간 정상 경로 한번 돌려보기

다음 순서대로 Swagger 또는 curl 로 진행하면 주요 기능을 한 번에 훑을 수 있습니다.

1. **signup** — `test@example.com` 계정 생성
2. **login** — access/refresh 토큰 수신
3. **`/api/users/me`** — 인증 성공 확인 (`200`)
4. **`/api/admin/users`** — 권한 차단 확인 (`403`)
5. **H2 콘솔** — `ROLE_ADMIN` 부여
6. **login** 재실행 — 새 access 토큰 수신
7. **`/api/admin/users`** — 이번엔 `200` 과 사용자 목록
8. **refresh** — 기존 refresh 로 새 토큰 쌍 수신
9. **refresh** 다시 (같은 토큰) — `401` 확인 (rotation 동작)
10. **logout** — 현재 refresh 폐기

여기까지 막힘없이 통과하면 로컬 + OAuth 이외 전 기능이 정상입니다.
