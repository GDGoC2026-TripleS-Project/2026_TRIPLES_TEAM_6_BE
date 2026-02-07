# ☕ LastCup Backend

카페인·당 섭취를 기록하고, 하루 목표 대비 현황을 추적할 수 있는 서비스의 백엔드 API 서버입니다.

> **API Server** — https://api.lastcup.site  
> **Swagger UI** — https://api.lastcup.site/swagger-ui.html

<br>

## 팀 컨벤션

📎 [CONVENTION.md](CONVENTION.md) — 브랜치 전략, 커밋 메시지, 코드 컨벤션, 메서드 네이밍

<br>

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.1 |
| ORM / DB | Spring Data JPA · MySQL |
| Auth | JWT (jjwt 0.12) · BCrypt · OAuth 2.0 (Kakao, Google, Apple) |
| Storage | AWS S3 |
| Docs | springdoc-openapi (Swagger UI) |
| Infra | Docker · Kubernetes · Argo CD (Blue-Green) · GHCR |

<br>

## 프로젝트 구조

도메인별 패키지를 분리하고, 각 도메인 내부는 `controller → service → repository → domain` 레이어로 구성합니다.

```
src/main/java/com/lastcup/api
├── domain
│   ├── auth        # 인증 (로컬 회원가입/로그인, 소셜 로그인, 비밀번호 재설정)
│   ├── brand       # 브랜드 (카페 브랜드 조회)
│   ├── goal        # 목표 설정 (일일 카페인/당 목표)
│   ├── intake      # 섭취 기록 (일별·기간별 조회, 영양 스냅샷)
│   ├── menu        # 메뉴 (메뉴·사이즈·영양성분 조회)
│   ├── option      # 옵션 (시럽, 샷, 크림 등)
│   └── user        # 유저 (프로필, 기기, 알림 설정, 즐겨찾기)
├── global
│   ├── config      # 공통 설정 (Swagger, JPA Auditing, Jackson)
│   ├── error       # 에러 코드 · GlobalExceptionHandler
│   └── response    # 표준 응답 Envelope (ApiResponse, ApiError)
├── infrastructure
│   ├── oauth       # 소셜 로그인 클라이언트 (Kakao, Google, Apple)
│   └── storage     # S3 파일 업로드
└── security        # JWT 발급/검증, Security Filter Chain
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

| 변수 | 설명 |
|---|---|
| `DB_URL` | MySQL JDBC URL |
| `DB_USER` / `DB_PASS` | DB 접속 정보 |
| `JWT_SECRET_KEY` | JWT 서명 키 (HS256, 32byte 이상) |
| `AWS_S3_BUCKET` | S3 버킷명 |
| `GOOGLE_CLIENT_IDS` | Google OAuth Client ID |
| `KAKAO_CLIENT_ID` | Kakao REST API Key |
| `APPLE_CLIENT_ID` | Apple Service ID |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP 메일 계정 (비밀번호 재설정용) |

### 2. 실행

```bash
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 시작됩니다.  
Swagger UI는 `http://localhost:8080/swagger-ui.html` 에서 확인할 수 있습니다.

<br>

## 배포 파이프라인

```
main 브랜치 push
  → GitHub Actions → Docker build → GHCR push
    → Argo CD Image Updater (digest 감지)
      → Argo Rollout Blue-Green 배포
```

- **Blue-Green 전략**: preview 서비스에 신규 버전 배포 후, health check 통과 시 자동 프로모션(승격)하여 무중단 배포 실현
- **Rollback**: Argo CD 대시보드 또는 `kubectl argo rollouts undo`로 즉시 가능

<br>

---
EOF


