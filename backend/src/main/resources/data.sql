INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT '실시간 채팅 서비스', 'WebSocket 기반 실시간 채팅 애플리케이션 구현 (Spring Boot + Redis Pub/Sub)', 'PENDING', 'Phase1', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = '실시간 채팅 서비스');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'API Gateway 구현', 'Spring Cloud Gateway를 활용한 API Gateway 구현 (라우팅, 인증, 속도 제한)', 'PENDING', 'Phase1', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'API Gateway 구현');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'OAuth2/OIDC 인증 서버', 'Spring Authorization Server 기반 OAuth2/OIDC 인증 서버 구축', 'PENDING', 'Phase1', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'OAuth2/OIDC 인증 서버');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'K8s 마이크로서비스 배포', 'Kubernetes 클러스터에 마이크로서비스 아키텍처 배포 (Helm Chart, Service Mesh)', 'PENDING', 'Phase2', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'K8s 마이크로서비스 배포');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'CI/CD 파이프라인 구축', 'Jenkins/GitHub Actions 기반 CI/CD 파이프라인 구축 (빌드, 테스트, 배포 자동화)', 'PENDING', 'Phase2', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'CI/CD 파이프라인 구축');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT '모니터링/옵저버빌리티 스택', 'Prometheus + Grafana + ELK 스택을 활용한 모니터링 및 로깅 시스템 구축', 'PENDING', 'Phase2', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = '모니터링/옵저버빌리티 스택');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'Go CLI 도구', 'Go 언어로 개발자 생산성 향상을 위한 CLI 도구 개발 (Cobra, Viper)', 'PENDING', 'Phase3', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'Go CLI 도구');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'Rust 프록시 서버', 'Rust로 고성능 리버스 프록시 서버 구현 (Tokio, Hyper)', 'PENDING', 'Phase3', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'Rust 프록시 서버');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'Next.js 풀스택 앱', 'Next.js App Router 기반 풀스택 웹 애플리케이션 개발 (SSR, API Routes)', 'PENDING', 'Phase3', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'Next.js 풀스택 앱');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT '이벤트 드리븐 주문 시스템', 'Kafka/RabbitMQ 기반 이벤트 드리븐 주문 처리 시스템 (CQRS, Event Sourcing)', 'PENDING', 'Phase4', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = '이벤트 드리븐 주문 시스템');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT '분산 캐시+DB 동기화', 'Redis 분산 캐시와 DB 동기화 전략 구현 (Cache-Aside, Write-Through, Write-Behind)', 'PENDING', 'Phase4', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = '분산 캐시+DB 동기화');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT '검색 엔진 연동', 'Elasticsearch 기반 전문 검색 엔진 연동 (인덱싱, 한국어 형태소 분석)', 'PENDING', 'Phase4', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = '검색 엔진 연동');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'RAG 기반 문서 QA 서비스', 'LLM + Vector DB를 활용한 RAG 기반 문서 질의응답 서비스 구축', 'PENDING', 'Phase5', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'RAG 기반 문서 QA 서비스');

INSERT INTO todo_items (title, description, status, phase, created_at, updated_at)
SELECT 'AI Agent 워크플로우', 'LangChain/LangGraph 기반 AI Agent 워크플로우 시스템 구현', 'PENDING', 'Phase5', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM todo_items WHERE title = 'AI Agent 워크플로우');
