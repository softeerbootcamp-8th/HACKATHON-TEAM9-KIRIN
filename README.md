# HACKATHON-TEAM9-KIRIN

## 프로젝트 구조

```
.
├── backend/    # Spring Boot 애플리케이션 (Java 21, Gradle)
└── frontend/   # 프론트엔드 애플리케이션
```

## Backend

- Java 21 / Spring Boot 4.1.1 / Gradle 9.5.1
- Spring Web MVC, Spring Data JPA, Lombok

### 실행

```bash
cd backend
./gradlew bootRun
```

### 테스트

```bash
cd backend
./gradlew test
```

## Frontend

- React 19 / TypeScript / Vite, TanStack Router · Query, Tailwind v4 + shadcn/ui
- 자세한 규칙은 [`frontend/docs/RULESET.md`](frontend/docs/RULESET.md), 디자인 토큰은
  [`frontend/DESIGN.md`](frontend/DESIGN.md) 참조

### 실행

```bash
cd frontend
pnpm install
pnpm dev
```
