# CLAUDE.md — tj-blog 작업 런북

다른 Claude 세션이 이 저장소를 바로 이어받아 **실행/검증/개발**할 수 있도록 정리한 문서.
(repo: `taejinmom/tj-blog`, 작업 클론: `/mnt/c/Users/taejin/tj-blog`)

---

## ⚠️ 환경 (가장 중요)

- **WSL2(Ubuntu)** 환경. bare `docker` 명령은 안 됨 ("command could not be found in this WSL 2 distro" — Docker Desktop의 WSL 통합 꺼짐).
- **반드시 `docker.exe` / `docker.exe compose` 사용** (Windows 엔진에 연결됨).
- Docker Desktop이 꺼져 있으면 `docker.exe ps`가 `daemon running?` 에러 → 기동:
  ```bash
  powershell.exe -Command "Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'"
  # 엔진 준비 대기
  for i in $(seq 1 50); do docker.exe ps >/dev/null 2>&1 && { echo ready; break; }; sleep 3; done
  ```
- compose 빌드/볼륨 마운트가 깨끗하려면 repo가 Windows 경로(`/mnt/c/...`)에 있어야 함. `docker.exe compose -f /mnt/c/...` 처럼 WSL 경로를 `-f`로 주면 `C:\mnt\c...`로 오인하니, **`cd` 후 인자 없이 실행**할 것.
- Java 17 / Node 20 / Python3 사용 가능. `gh`는 `taejinmom` 계정으로 로그인됨.

---

## 프로젝트 개요

마이크로서비스 포트폴리오 플랫폼 (블로그 + 로드맵 + 실시간 채팅).

| 컴포넌트 | 경로 | 포트 | 스택 |
|----------|------|------|------|
| frontend | `frontend/` | 80(nginx) / 5173(dev) | React19 + Vite8 + Tailwind4 |
| blog | `services/blog/` | 8080 | Spring Boot 3.2.5, JPA, 게시물/로드맵/댓글 |
| auth | `services/auth/` | 8082 | JWT(Access/Refresh), OAuth2, RBAC |
| chat | `services/chat/` | 8081 | WebSocket(STOMP) + Redis Pub/Sub |
| db | postgres:15 | 5432 | blogdb / authdb / chatdb |
| redis | redis:7 | 6379 | chat pub/sub |

nginx가 `/api/posts,/todos`→blog, `/api/auth,/users,/admin`→auth, `/api/chat-*,/ws`→chat 로 프록시.

---

## ▶️ 전체 실행 (Docker)

```bash
cd /mnt/c/Users/taejin/tj-blog
export JWT_SECRET="$(openssl rand -base64 48)"   # 미지정 시 약한 기본값
docker.exe compose up -d --build
docker.exe compose ps
```
접속: 웹 http://localhost · blog http://localhost:8080/api · auth :8082 · chat :8081

> 참고: 운영 실행본이 `/home/taejin/projects/portfolio-blog`(compose project=`portfolio-blog`, `restart: unless-stopped`)에도 있어, Docker Desktop이 켜지면 자동 기동됨. **컨테이너 이름이 고정(container_name)**이라 두 위치에서 동시에 `up` 하면 이름 충돌 → 한쪽만 띄울 것.

### 로컬 개발
```bash
docker.exe compose up -d platform-db platform-redis
cd services/blog && ./gradlew bootRun   # 8080 (auth=8082, chat=8081 동일 방식)
cd frontend && npm install && npm run dev  # 5173, /api 프록시는 vite.config.ts
```

---

## ✅ 검증 / 테스트

```bash
# 프론트
cd frontend && npm ci && npm run lint && npm run build

# 백엔드 (각 서비스에 gradle wrapper 있음)
cd services/blog && ./gradlew test --no-daemon
cd services/chat && ./gradlew test --no-daemon
cd services/auth && ./gradlew test --no-daemon
```

### 운영 DB를 건드리지 않고 백엔드 한 개만 격리 검증하는 패턴
```bash
docker.exe compose build blog-backend
NET=$(docker.exe network ls --format '{{.Name}}' | grep portfolio-blog_platform-network)
docker.exe run -d --name verify --network "$NET" -p 18080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://platform-db:5432/blogdb \
  -e SPRING_DATASOURCE_USERNAME=blog -e SPRING_DATASOURCE_PASSWORD=blog1234 \
  -e JWT_SECRET=change-me-change-me-change-me-change-me-change-me-1234 tj-blog-blog-backend
# ... curl.exe http://localhost:18080/... 로 테스트 후
docker.exe rm -f verify
```

### 인증 필요한 API 테스트용 JWT 직접 발급 (HS256, UTF-8 시크릿)
```bash
python3 - <<'PY'
import json,hmac,hashlib,base64,time
secret="change-me-change-me-change-me-change-me-change-me-1234"
b64=lambda b: base64.urlsafe_b64encode(b).rstrip(b'=').decode()
h=b64(json.dumps({"alg":"HS256","typ":"JWT"},separators=(',',':')).encode()); n=int(time.time())
# chat 서비스는 "typ":"access" 클레임이 반드시 있어야 토큰을 받음! blog는 없어도 됨.
p=b64(json.dumps({"sub":"1","email":"admin@e.com","roles":["ROLE_ADMIN"],"typ":"access","iat":n,"exp":n+3600},separators=(',',':')).encode())
print(f"{h}.{p}."+b64(hmac.new(secret.encode(),f"{h}.{p}".encode(),hashlib.sha256).digest()))
PY
```

---

## 현재 상태 (2026-05-26)

**열린 PR 2개** (stacked):
- **#1 `fix/api-errors-and-dev-proxy`** → main : 버그 수정(404 처리, vite 프록시 host, 죽은 채팅 링크, 린트, README 갱신).
- **#2 `feat/enhancements`** → base가 **#1 브랜치** : 기능 추가. #1 머지되면 자동으로 main 대상 전환.
- **권장 머지 순서: #1 먼저 → #2.** CI(루트 `.github/workflows/ci.yml`)는 #2에서 4잡(frontend+blog+auth+chat) 전부 green 확인됨.

**#2에 담긴 기능**
1. 블로그: 서버 페이지네이션(`GET /api/posts?page&size&q&category&tag`), 검색, 태그 UI, Swagger(blog·chat), Actuator health(blog)
2. 블로그 댓글: `GET/POST /api/posts/{id}/comments`, `DELETE /api/comments/{id}`(작성자/관리자)
3. 채팅 방 목록에 사용자별 `unreadCount`+`lastMessage`
4. 테스트(blog/chat) + 루트 GitHub Actions CI + blog/chat gradle wrapper 추가

**작업 중 함께 고친 버그**
- blog JWT 필터가 역할에 `ROLE_` 중복 부착(`ROLE_ROLE_ADMIN`) → 관리자 글쓰기 막혀있던 것 수정.
- 검색 시 Postgres `lower(bytea)` 오류로 401 나던 쿼리 → 파라미터별 분기.

---

## 알려진 이슈 / 다음 할 일 후보

- `services/auth/.github/workflows/ci.yml` 은 서브디렉토리라 **GitHub에서 실행 안 되는 dead 워크플로우**. 루트 CI로 일원화했으니 삭제 가능.
- 관리자 계정은 가입 시 안 만들어짐 → `authdb`에서 수동 `ROLE_ADMIN` 부여 후 재로그인 필요. (시드 admin 만들기 = 개선 후보)
- 미구현 개선 후보: 블로그 이미지 업로드, API Gateway(Spring Cloud Gateway), 관측성(Prometheus/Grafana), 운영용 CORS/시크릿 강화, 프론트 테스트(vitest).
- 채팅 메시지 전송은 REST 엔드포인트가 없고 **WebSocket(STOMP)** 으로만 가능 (`/ws`). curl로는 방 생성/입장/조회까지만 테스트됨.

---

## 명령 치트시트
```bash
docker.exe compose ps                      # 상태
docker.exe compose logs -f blog-backend    # 로그
docker.exe compose down                    # 종료(데이터 유지) / down -v (초기화)
gh pr list --state open                    # 열린 PR
gh pr checks 2                             # CI 상태
curl.exe -s http://localhost:8080/api/posts   # WSL→Windows 포트는 curl.exe 권장
```
