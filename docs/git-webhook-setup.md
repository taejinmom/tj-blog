# Git Webhook 설정 가이드

## 1. Jenkins 초기 설정

### Jenkins 접속
- URL: `http://localhost:9090`
- 초기 비밀번호 확인:
  ```bash
  docker exec blog-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
  ```

### Jenkins Pipeline Job 생성
1. **새 Item** > **Pipeline** 선택 > 이름: `portfolio-blog`
2. **Build Triggers** > **GitHub hook trigger for GITScm polling** 체크
3. **Pipeline** > **Pipeline script from SCM** 선택
   - SCM: Git
   - Repository URL: GitHub 저장소 URL 입력
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`
4. 저장

## 2. GitHub Webhook 설정

### GitHub 저장소에서 Webhook 추가
1. GitHub 저장소 > **Settings** > **Webhooks** > **Add webhook**
2. 설정값:
   - **Payload URL**: `http://<서버IP>:9090/github-webhook/`
   - **Content type**: `application/json`
   - **Secret**: (선택사항) Jenkins에서 설정한 시크릿
   - **Which events**: `Just the push event`
3. **Add webhook** 클릭

### 로컬 개발 환경에서 테스트 (ngrok 사용)
로컬에서 Jenkins를 실행 중이라면 ngrok으로 외부 접근 가능하게 설정:
```bash
ngrok http 9090
```
생성된 URL을 GitHub Webhook의 Payload URL로 사용합니다.

## 3. Jenkins Credentials 설정

### GitHub 인증 정보 추가
1. **Jenkins 관리** > **Credentials** > **System** > **Global credentials**
2. **Add Credentials** 클릭
   - Kind: **Username with password**
   - Username: GitHub 사용자명
   - Password: GitHub Personal Access Token
   - ID: `github-credentials`

## 4. 동작 확인

1. 코드 수정 후 push:
   ```bash
   git add .
   git commit -m "test webhook"
   git push origin main
   ```
2. Jenkins 대시보드에서 빌드가 자동으로 트리거되는지 확인
3. 빌드 로그에서 각 스테이지 성공 여부 확인
