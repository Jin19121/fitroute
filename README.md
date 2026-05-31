# FitRoute — AI 기반 맞춤형 다이어트 & 운동 관리 플랫폼

> **"오늘의 행동을 구조화된 데이터로 전환하고, 지속적으로 추천을 개선하는 시스템"**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.x-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-DC382D?style=flat-square&logo=redis)](https://redis.io/)

---

## 목차

- [서비스 소개](#서비스-소개)
- [핵심 기능](#핵심-기능)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [도메인 모델](#도메인-모델)
- [API 설계](#api-설계)
- [프로젝트 구조](#프로젝트-구조)
- [환경 설정 및 실행](#환경-설정-및-실행)
- [시니어 레벨 설계 결정](#시니어-레벨-설계-결정)

---

## 서비스 소개

FitRoute는 사용자의 신체 정보와 목표를 기반으로 **AI(Google Gemini)** 가 매일 맞춤형 식단 및 운동 계획을 생성하고, 사용자의 실제 행동 로그를 **Plan vs Log** 구조로 비교·분석하는 헬스케어 플랫폼입니다.

### 핵심 플로우

```
회원가입 (신체정보 입력)
    ↓
AI 플랜 생성 (Gemini API)
    ↓
오늘 하루 계획 확인 (Dashboard)
    ↓
실행 체크 / 수정 기록 (Log)
    ↓
월간 리포트로 분석 (Report)
    ↓
AI 추천 품질 지속 개선 (Feedback Loop)
```

---

## 핵심 기능

### 🤖 AI 플랜 생성
- Google Gemini 2.5 Flash Lite 모델을 활용한 개인 맞춤 플랜 생성
- **식단**: DB에 등록된 식품 데이터를 AI가 선택 (실제 영양 정보 사용)
- **운동**: AI 자유 생성 방식으로 경험 수준/목표에 맞는 루틴 구성
- AI 응답은 반드시 서버 측에서 검증·정규화 후 저장

### 📊 대시보드 (Today)
- 오늘의 칼로리 섭취/소모 현황 실시간 집계
- PlanItem 단위 상태 관리: `PENDING → COMPLETED / SKIPPED / (MODIFY)`
- 낙관적 UI 업데이트로 즉각적인 사용자 피드백

### 📝 Plan vs Log 비교
- 계획(PlanItem)과 실제 기록(LogItem)을 별도 테이블로 관리
- 수정 이력 보존 (`modifiedName`, `modifiedCalories` 등)
- `diffCalories` 자동 계산으로 계획 대비 실제 편차 추적

### 📅 월간 리포트
- 캘린더 형태의 운동/식단/체중 달성 현황 시각화
- KPI 요약: 달성일수, 평균 섭취 칼로리, 체중 변화량
- 날짜 클릭 시 해당 일자 Plan vs Log 상세 카드 표시

### ⚖️ 체중 관리
- 체중/체지방률/골격근량 기록 (upsert 방식)
- 월간 체중 추이 차트 (SVG 자체 구현)

---

## 기술 스택

### Backend

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x |
| ORM | Spring Data JPA + Hibernate 6 |
| DB | MySQL 8.0 |
| Cache / Session | Redis 7.2 (Lettuce) |
| Security | Spring Security + JWT (JJWT 0.12.x) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| AI | Google Gemini API (generativelanguage) |
| Build | Gradle 8 |

### Frontend

| 구분 | 기술 |
|------|------|
| Framework | React 19 + Vite 8 |
| Styling | Tailwind CSS 3.4 |
| State | Zustand 5 |
| HTTP | Axios 1.15 (interceptor 기반 silent refresh) |
| Router | React Router DOM 7 |

### Infrastructure

| 구분 | 기술 |
|------|------|
| Container | Docker + Docker Compose |
| DB | MySQL 8.0 (utf8mb4) |
| Cache | Redis 7.2 |

---

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    React (Vite)                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐  │
│  │Dashboard │ │  Diet    │ │ Workout  │ │  Report   │  │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘  │
│           Zustand (planStore / authStore)                │
│              Axios (JWT interceptor, silent refresh)      │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP REST
┌──────────────────────▼──────────────────────────────────┐
│              Spring Boot 3.5 (Port 8080)                 │
│                                                          │
│  Controller → Service → Repository                       │
│                                                          │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ AuthService │  │DailyPlanSvc  │  │ DashboardSvc   │  │
│  │ (JWT+Redis) │  │(AI+DB Plan)  │  │ (Log 집계)     │  │
│  └─────────────┘  └──────┬───────┘  └────────────────┘  │
│                           │                              │
│  ┌─────────────┐  ┌──────▼───────┐  ┌────────────────┐  │
│  │ LogService  │  │ GeminiClient │  │ ReportService  │  │
│  │(Plan→Log)   │  │(AI 연동)     │  │(월간 집계)     │  │
│  └─────────────┘  └──────────────┘  └────────────────┘  │
│                                                          │
│  AES-256-GCM (이메일 암호화) + SHA-256 (검색용 해시)    │
└───────────┬──────────────────┬──────────────────────────┘
            │                  │
    ┌───────▼──────┐   ┌───────▼──────┐
    │  MySQL 8.0   │   │  Redis 7.2   │
    │  (JPA/ORM)   │   │  (RT 저장)   │
    └──────────────┘   └──────────────┘
                       │
               ┌───────▼──────────┐
               │  Google Gemini   │
               │  API (Flash Lite) │
               └──────────────────┘
```

---

## 도메인 모델

### ERD (주요 엔티티)

```
users
├── id (PK)
├── encrypted_email (AES-256-GCM)
├── email_hash (SHA-256, 인덱스)
└── password (BCrypt)

user_profiles
├── id (PK)
├── user_id (FK → users)
├── height, weight, target_weight, target_period
├── gender, birth_date
├── activity_level, goal_type, exercise_experience, diet_style
└── ...

daily_plans
├── id (PK)
├── user_id, plan_date
├── status: PENDING|GENERATING|ACTIVE|COMPLETED|SUPERSEDED|FAILED
├── version (재생성 버전 관리)
├── root_plan_id (같은 날 첫 버전 ID)
├── calorie_target
├── meal_plan (JSON)
├── workout_plan (JSON)
└── ai_meta (JSON)

plan_items
├── id (PK)
├── plan_id (FK → daily_plans)
├── type: MEAL | WORKOUT
├── category: BREAKFAST|LUNCH|DINNER|SNACK|CHEST|BACK|...
├── status: PENDING | COMPLETED | SKIPPED
├── calories, protein, carbs, fat
├── modified_name, modified_calories, ... (수정 이력)
├── is_modified (boolean)
└── food_name / exercise_name

logs
├── id (PK)
├── user_id, daily_plan_id, log_date
├── consumed_calories, burned_calories
└── completion_rate

log_items
├── id (PK)
├── log_id (FK → logs)
├── plan_item_id (FK → plan_items)
├── original_name, actual_name
├── original_calories, actual_calories, diff_calories
└── status: COMPLETED | SKIPPED

weight_logs
├── id (PK)
├── user_id, log_date (UNIQUE)
├── weight_kg, body_fat_pct, muscle_mass
└── note

foods
├── id (PK)
├── food_code (식약처 코드, UNIQUE)
├── name, category, serving_size
├── calories, protein, fat, carbs
└── tags (AI 필터링용: "고단백,저지방")
```

### Plan 버전 관리 전략

```
Day 1: daily_plans v1 (ACTIVE)
         ↓ 재생성 트리거
Day 1: daily_plans v1 (SUPERSEDED) → v2 (ACTIVE)
         root_plan_id = v1.id (계보 추적)
```

---

## API 설계

### 인증 (`/api/auth`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/signup` | 회원가입 + JWT 발급 |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/refresh` | 액세스 토큰 갱신 |
| POST | `/api/auth/logout` | 로그아웃 (Redis RT 삭제) |

**회원가입 요청 예시:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "height": 170.0,
  "weight": 75.0,
  "targetWeight": 65.0,
  "targetPeriod": 12,
  "gender": "MALE",
  "birthDate": "1995-03-15",
  "activityLevel": "MODERATELY_ACTIVE",
  "goalType": "WEIGHT_LOSS",
  "exerciseExperience": "INTERMEDIATE",
  "dietStyle": "LOW_CALORIE"
}
```

### 플랜 (`/api/plans`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/plans/today/generate` | 오늘 AI 플랜 생성 |
| GET | `/api/plans/today` | 오늘 플랜 조회 |
| POST | `/api/plans/items` | 항목 직접 추가 |
| PATCH | `/api/plans/items/{id}/action` | 항목 상태 변경 |
| GET | `/api/plans/workout/weekly` | 주간 운동 계획 |

**액션 요청 예시 (항목 완수 + 수정):**
```json
{
  "action": "COMPLETE_WITH_MODIFY",
  "modifiedName": "현미밥 + 불고기",
  "modifiedCalories": 580,
  "modifiedProtein": 35,
  "modifiedCarbs": 72,
  "modifiedFat": 12
}
```

**지원 액션:**

| Action | 설명 |
|--------|------|
| `COMPLETE` | 계획대로 완수 |
| `SKIP` | 건너뛰기 |
| `MODIFY` | 수정만 기록 (PENDING 유지) |
| `COMPLETE_WITH_MODIFY` | 수정 후 완수 |
| `RESET` | PENDING 상태로 초기화 |

### 대시보드 (`/api/dashboard`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/dashboard/today` | 오늘 전체 현황 |

### 리포트 (`/api/reports`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/reports/monthly?year=&month=` | 월간 캘린더 + KPI |
| GET | `/api/reports/daily/{date}` | 일자 상세 (Plan vs Log) |

### 체중 (`/api/weight-logs`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/weight-logs` | 기록 (upsert) |
| GET | `/api/weight-logs/today` | 오늘 체중 |
| GET | `/api/weight-logs/latest` | 최근 체중 1건 |
| GET | `/api/weight-logs?year=&month=` | 월별 목록 |
| DELETE | `/api/weight-logs/{date}` | 삭제 |

---

## 프로젝트 구조

### Backend

```
fitroute-backend/
└── src/main/java/com/fitroute/
    ├── FitrouteApplication.java
    ├── domain/
    │   ├── user/
    │   │   ├── controller/   AuthController
    │   │   ├── service/      AuthService
    │   │   ├── entity/       User, UserProfile
    │   │   ├── repository/   UserRepository, UserProfileRepository
    │   │   ├── dto/          SignupRequest, LoginRequest, TokenResponse ...
    │   │   └── event/        UserSignedUpEvent
    │   ├── plan/
    │   │   ├── controller/   DailyPlanController, DashboardController, PlanController
    │   │   ├── service/      DailyPlanService, DashboardService, PlanGenerationService
    │   │   ├── entity/       DailyPlan, PlanItem, Plan
    │   │   ├── repository/   DailyPlanRepository, PlanItemRepository
    │   │   └── dto/          DailyPlanResponse, DashboardResponse, PlanItemActionRequest ...
    │   ├── log/
    │   │   ├── service/      LogService
    │   │   ├── entity/       Log, LogItem
    │   │   └── repository/   LogRepository, LogItemRepository
    │   ├── report/
    │   │   ├── controller/   ReportController
    │   │   ├── service/      ReportService
    │   │   └── dto/          MonthlyReportResponse, DailyReportResponse
    │   ├── weight/
    │   │   ├── controller/   WeightLogController
    │   │   ├── service/      WeightLogService
    │   │   ├── entity/       WeightLog
    │   │   └── dto/          WeightLogRequest, WeightLogResponse
    │   └── food/
    │       ├── entity/       Food
    │       └── repository/   FoodRepository
    └── global/
        ├── ai/               GeminiClient, GeminiPromptBuilder, GeminiResponseParser
        ├── config/           SecurityConfig, RedisConfig, SwaggerConfig
        ├── enums/            PlanItemStatus, PlanItemAction, ActivityLevel ...
        ├── exception/        GlobalExceptionHandler, ErrorCode
        ├── jwt/              JwtProvider, JwtAuthenticationFilter
        └── util/             Aes256Util, CalorieCalculator
```

### Frontend

```
fitroute-frontend/src/
├── api/          axios.js, auth.js, diet.js, workout.js, weight.js, report.js
├── components/
│   ├── common/   BottomNav, QuickAddFab, Input, Button, Chips ...
│   ├── diet/     MealSection
│   ├── layout/   PhoneFrame, PrivateRoute
│   ├── report/   MonthCalendar, DayDetailCard, KpiCards, WeightChart
│   └── workout/  WorkoutItem, WorkoutAddSheet, WorkoutDetailSheet
├── hooks/        useAuth, useReport, useWorkoutToday, useCalendar
├── pages/
│   ├── onboarding/   LoginPage, SignupPage, AiSetupPage, AiLoadingPage
│   ├── dashboard/    DashboardPage
│   ├── diet/         DietPage, DietTodayTab, FoodItem
│   ├── workout/      WorkoutPage, WorkoutTodayTab, WorkoutPlanTab
│   └── report/       ReportPage
├── store/        authStore.js, planStore.js, onboardingStore.js
└── utils/        validators.js
```

---

## 환경 설정 및 실행

### 사전 요구사항

- Java 21+
- Node.js 20+
- Docker & Docker Compose

### 1. 인프라 실행 (MySQL + Redis)

```bash
cd fitroute-backend
docker-compose up -d
```

### 2. 환경 변수 설정 (`.env`)

```bash
# fitroute-backend/.env
DB_URL=jdbc:mysql://localhost:3306/fitroute?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
DB_USERNAME=fitroute
DB_PASSWORD=your_db_password
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=fitroute
MYSQL_USER=fitroute
MYSQL_PASSWORD=your_db_password

JWT_SECRET=your-jwt-secret-key-at-least-32-chars
AES_KEY=your-aes-256-key-exactly-32-bytes!

GOOGLE_API_KEY=your-gemini-api-key

REDIS_HOST=localhost
REDIS_PORT=6379
```

### 3. 백엔드 실행

```bash
cd fitroute-backend
./gradlew bootRun
# http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui/index.html
```

### 4. 프론트엔드 실행

```bash
cd fitroute-frontend
npm install
npm run dev
# http://localhost:3000
```

### 5. 식품 데이터 적재 (선택)

공공데이터포탈 Open API 또는 CSV를 통해 `foods` 테이블에 데이터를 적재합니다.
AI 식단 추천 기능은 이 데이터를 기반으로 동작합니다.

---

## 심화 설계

### 1. 보안 — 이메일 이중 처리

```
평문 이메일
  ├── AES-256-GCM 암호화 → encrypted_email (복호화 가능, 마스킹 표시용)
  └── SHA-256 해시       → email_hash (인덱스 기반 O(1) 검색)
```
- 전체 유저 스캔 없이 로그인/중복체크 가능
- GCM 모드: IV를 암호문 앞에 붙여 동일 입력도 매번 다른 암호문 생성

### 2. JWT + Redis Refresh Token

- Access Token: `sessionStorage` (탭 종료 시 소멸)
- Refresh Token: `localStorage` + Redis 서버 측 검증
- 동시 다중 요청 시 단 한 번만 갱신 (pendingQueue 패턴)
- 로그아웃 즉시 Redis에서 RT 삭제 → 강제 만료

### 3. Plan 버전 관리

플랜 재생성 시 기존 플랜을 삭제하지 않고 `SUPERSEDED` 처리하여:
- 분석/비교를 위한 이력 보존
- `root_plan_id`로 같은 날의 플랜 계보 추적 가능

### 4. AI 응답 검증 파이프라인

```
Gemini API 호출
    ↓
GeminiResponseParser (구조 파싱)
    ↓
필수 필드 검증 (meal_plan, workout_plan 존재 여부)
    ↓
칼로리 재합산 (AI 계산 오류 방지)
    ↓
카테고리 코드 → Enum 변환 (알 수 없는 코드 스킵)
    ↓
DB 확정값으로 영양 정보 덮어쓰기 (식단)
    ↓
PlanItem 저장
```

### 5. Plan vs Log 이중 구조

| 레이어 | 역할 |
|--------|------|
| `PlanItem` | AI가 생성한 원본 계획 + 사용자 수정 이력 보존 |
| `LogItem` | 실제 기록 (actual vs original diff 계산) |
| `Log` | 일별 집계 (consumed/burned calories, completion_rate) |

이 구조 덕분에 "계획한 것 vs 실제 한 것"을 언제든 정확하게 비교 가능합니다.

### 6. 낙관적 UI 업데이트

```javascript
// 1. 즉시 UI 반영 (서버 응답 대기 X)
get().updatePlanItem(itemId, buildOptimisticPatch(action, fields));

// 2. 서버 API 호출
await patchPlanItem(itemId, payload);

// 3. 실패 시 서버 데이터로 롤백
catch (e) => { fetchToday(true); }
```

### 7. 칼로리 계산 (Harris-Benedict)

```
BMR(남) = 88.362 + (13.397 × W) + (4.799 × H) - (5.677 × A)
BMR(여) = 447.593 + (9.247 × W) + (3.098 × H) - (4.330 × A)

TDEE = BMR × 활동 계수
목표 칼로리 = TDEE - 일일 적자 (안전 하한선 적용)
```

- 남성 최소 1,500 kcal / 여성 최소 1,200 kcal 보장
- 감량 속도: TDEE 85% 상한선으로 급격한 다이어트 방지

### 8. Enum 기반 상태 머신

boolean 플래그를 일절 사용하지 않고 모든 상태를 Enum으로 관리:

```java
// PlanItemStatus
PENDING → COMPLETED
PENDING → SKIPPED
any    → PENDING (RESET)

// DailyPlan.PlanStatus
GENERATING → ACTIVE
GENERATING → FAILED
ACTIVE     → SUPERSEDED (재생성 시)
```

---

## 라이선스

본 프로젝트는 학습 및 포트폴리오 목적으로 제작되었습니다.

---

<p align="center">
  Made with ❤️ by Jimin Kim
</p>
