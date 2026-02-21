# LastCup Backend

카페인과 당 섭취를 기록하고, 하루 목표 대비 현황을 추적할 수 있는 `'라스트컵'` 서비스의 백엔드 API 서버입니다.

<br>

## 팀 컨벤션

[CONVENTION.md](CONVENTION.md) — 브랜치 전략, 커밋 메시지, 코드 컨벤션, 메서드 네이밍

<br>

## 기술 스택

| 분류                   | 기술 |
|----------------------|---|
| Language / Framework | Java 17 · Spring Boot 4.0.1 |
| ORM / DB             | Spring Data JPA · MySQL |
| Auth                 | JWT (jjwt 0.12.5) · BCrypt · OAuth 2.0 (Kakao, Google, Apple) · nimbus-jose-jwt 10.0.2 |
| 푸시알림                 | Firebase Admin SDK 9.2.0 (FCM) |
| Storage              | AWS S3 (AWS SDK 2.25.66) |
| Docs / Monitoring    | springdoc-openapi 2.7.0 (Swagger UI) · Spring Boot Actuator |
| Infra                | Docker · Kubernetes · Argo Rollouts (Blue-Green) · Argo CD · GHCR |

<br>

## 프로젝트 구조

도메인별 패키지를 분리하고, 각 도메인 내부는 `controller → service → repository → domain` 레이어로 구성합니다.

```
src/main/java/com/lastcup/api
├── domain
│   ├── auth           # 인증 (로컬 회원가입/로그인, 소셜 로그인, 비밀번호 재설정)
│   ├── brand          # 브랜드 (카페 브랜드 조회)
│   ├── goal           # 목표 설정 (일일 카페인/당 목표)
│   ├── intake         # 섭취 기록 (일별·기간별 조회, 영양 스냅샷)
│   ├── menu           # 메뉴 (메뉴·사이즈·영양성분 조회)
│   ├── notification   # 알림 (발송 이력, 스케줄링)
│   ├── option         # 옵션 (시럽, 샷, 크림 등)
│   └── user           # 유저 (프로필, 기기, 알림 설정, 즐겨찾기)
├── global
│   ├── config         # 공통 설정 (Swagger, JPA Auditing, Jackson, 타임존)
│   ├── error          # 에러 코드 · GlobalExceptionHandler
│   └── response       # 표준 응답 Envelope (ApiResponse, ApiError)
├── infrastructure
│   ├── notification   # FCM 푸시 알림 (Firebase Admin SDK)
│   ├── oauth          # 소셜 로그인 클라이언트 (Kakao, Google, Apple)
│   └── storage        # S3 파일 업로드
└── security           # JWT 발급/검증, Security Filter Chain
```

<br>

## API 응답 형식

모든 API는 아래 Envelope 형식으로 응답합니다.

**성공**
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-02-08T14:30:00"
}
```

**실패**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_VALIDATION_FAILED",
    "message": "요청 값이 유효하지 않습니다.",
    "fieldErrors": [
      { "field": "loginId",
        "reason": "필수 입력값입니다.",
        "rejectedValue": "" }
    ]
  },
  "timestamp": "2026-02-08T14:30:00"
}
```

<br>

## 로컬 실행

### 1. 환경 변수 설정

`k8s/local/01-secret.plain.yaml.example`을 참고하여 아래 환경 변수를 설정합니다.

| 변수 | 설명 | 기본값 |
|---|---|---|
| `DB_URL` | MySQL JDBC URL | — |
| `DB_USER` / `DB_PASS` | DB 접속 정보 | — |
| `JWT_SECRET_KEY` | JWT 서명 키 (HS256, 32byte 이상) | — |
| `AWS_S3_BUCKET` | S3 버킷명 | — |
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 | — |
| `AWS_SECRET_ACCESS_KEY` | AWS 시크릿 키 | — |
| `FIREBASE_ADMIN_KEY` | Firebase Admin SDK 키 (JSON) | — |
| `GOOGLE_CLIENT_IDS` | Google OAuth Client ID (쉼표 구분, 복수 가능) | — |
| `KAKAO_CLIENT_ID` | Kakao REST API Key | — |
| `KAKAO_CLIENT_SECRET` | Kakao Client Secret | — |
| `KAKAO_REDIRECT_URI` | Kakao 리다이렉트 URI | — |
| `KAKAO_REST_API_KEY` | Kakao REST API Key | — |
| `APPLE_CLIENT_ID` | Apple Service ID | — |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP 메일 계정 (비밀번호 재설정용) | — |
| `AWS_REGION` | S3 리전 | `ap-northeast-2` |
| `MAIL_HOST` / `MAIL_PORT` | SMTP 호스트·포트 | `smtp.naver.com` / `465` |

### 2. 실행

```bash
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 시작됩니다.
Swagger UI는 `http://localhost:8080/swagger-ui.html`에서 확인할 수 있습니다.

<br>

## 테스트

```bash
./gradlew test
```

| 분류 | 테스트 수 | 설명 |
|---|---|---|
| Service 단위 테스트 | 19 | 모든 Service 레이어 (Mockito 기반) |
| Repository 테스트 | 3 | DataJpaTest + H2 인메모리 DB |
| Context 로드 테스트 | 1 | 스프링 컨텍스트 정상 로드 확인 |

<br>

## 배포 파이프라인

### CI — 이미지 빌드

- GitHub Actions가 Docker 이미지를 빌드하고 GHCR에 push (`sha-{7자리}` + `latest` 태그)
- concurrency group으로 동시 배포 방지, 한 번에 하나만 실행

### CD — GitOps 배포

- Argo CD Image Updater가 GHCR에서 새 이미지 digest 감지
- Argo CD가 클러스터 상태를 Git 선언과 자동 동기화

### Blue-Green 무중단 배포

- 새 버전을 preview 서비스에 먼저 배포
- **Pre-Promotion 분석**: `/actuator/health` + `/api-docs` 를 10초 간격 6회 검증, 2회 초과 실패 시 승격 차단
  - 분석 통과 시 트래픽을 새 버전으로 자동 전환(승격)
- **Post-Promotion 분석**: active 서비스 대상으로 동일 검증 재수행, 실패 시 자동 롤백
  - 이전 버전 Pod은 70초간 유지하여 진행 중인 요청이 안전하게 완료되도록 보장

### 안정성 확보

- Pod 2개 유지 + `podAntiAffinity`로 노드 간 분산 배치
- `PodDisruptionBudget`으로 최소 1개 Pod 항상 유지
- Graceful Shutdown: Spring Boot 30초 + K8s 45초 이중 대기
- 수동 롤백: `kubectl argo rollouts undo`로 즉시 가능

<br>

### 시크릿 관리 — Sealed Secrets

DB 비밀번호, JWT 키, AWS 인증 정보 같은 민감한 값을 Git에 평문으로 올릴 수는 없으므로, Bitnami Sealed Secrets를 도입했습니다.

- Sealed Secrets Controller 설치 시 RSA 4096-bit 키 쌍 자동 생성
- 로컬에서 `kubeseal` CLI로 평문 Secret을 공개키로 암호화
- 암호화된 `SealedSecret` YAML을 Git에 커밋 (공개되어도 안전)
- ArgoCD 배포 시 Controller가 개인키로 복호화 → K8s Secret 생성

Git에는 암호화된 값만 존재하고, 복호화는 클러스터 안에서만 일어납니다. <br>
공개키(`k8s/lastcup/sealed-secrets/sealed-secrets-public.pem`)로 팀원 누구나 시크릿을 암호화할 수 있지만, 복호화는 클러스터만 가능합니다.

> 향후 External Secrets Operator + HashiCorp Vault 도입을 검토 중입니다. <br>
> 시크릿을 Git이 아닌 Vault에서 중앙 관리하고, 일정 주기마다 자동 회전(rotation)하여 보안을 강화할 계획입니다.
