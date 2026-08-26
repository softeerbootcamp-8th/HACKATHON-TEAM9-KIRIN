# HACKATHON-TEAM9-KIRIN — 오다가다

## 서비스 설명

**오다가다**는 물품보관함(사물함)을 매개로 하는 비대면 중고거래 서비스다.

- 판매자는 상품을 등록하고 빈 사물함을 예약한 뒤 실물을 넣고 잠근다.
- 구매자는 사물함 앞면에 붙은 QR 코드를 스캔해 그 사물함에 지금 들어있는 상품 정보를
  확인하고, 로그인 없이도 상품을 볼 수 있다.
- 결제(Toss Payments)가 완료되면 해당 사물함이 자동으로 열리고, 구매자는 직접 방문해
  물건을 수령한다.
- 별도의 대면 접촉이나 앱 설치 없이, 웹과 물리 사물함(ESP32 잠금장치)만으로 거래가
  완결된다.

## Tech Stack

| 구분 | 사용 기술 |
|:---:|---|
| **Backend** | ![Java 21](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot 4](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Spring Web MVC](https://img.shields.io/badge/Spring_Web_MVC-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Spring Validation](https://img.shields.io/badge/Spring_Validation-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Spring Actuator](https://img.shields.io/badge/Spring_Actuator-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Spring Security Crypto](https://img.shields.io/badge/Spring_Security_Crypto-6DB33F?style=flat-square&logo=springsecurity&logoColor=white) ![Lombok](https://img.shields.io/badge/Lombok-BC1723?style=flat-square&logoColor=white) |
| **Frontend** | ![React 19](https://img.shields.io/badge/React_19-61DAFB?style=flat-square&logo=react&logoColor=black) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white) ![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white) ![TanStack Query](https://img.shields.io/badge/TanStack_Query-FF4154?style=flat-square&logo=reactquery&logoColor=white) ![TanStack Router](https://img.shields.io/badge/TanStack_Router-CA4245?style=flat-square&logo=reactrouter&logoColor=white) ![Tailwind CSS 4](https://img.shields.io/badge/Tailwind_CSS_4-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white) ![shadcn/ui](https://img.shields.io/badge/shadcn%2Fui-000000?style=flat-square&logo=shadcnui&logoColor=white) ![Orval](https://img.shields.io/badge/Orval-7C3AED?style=flat-square&logo=openapiinitiative&logoColor=white) ![axios](https://img.shields.io/badge/axios-5A29E4?style=flat-square&logo=axios&logoColor=white) |
| **Embedded** | ![ESP32](https://img.shields.io/badge/ESP32-E7352C?style=flat-square&logo=espressif&logoColor=white) ![Arduino](https://img.shields.io/badge/Arduino_C%2B%2B-00979D?style=flat-square&logo=arduino&logoColor=white) |
| **Payment** | ![Toss Payments](https://img.shields.io/badge/Toss_Payments-0064FF?style=flat-square&logoColor=white) |
| **Database** | ![MySQL 8.4](https://img.shields.io/badge/MySQL_8.4-4479A1?style=flat-square&logo=mysql&logoColor=white) |
| **Infrastructure** | ![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=amazonwebservices&logoColor=white) ![Amazon VPC](https://img.shields.io/badge/Amazon_VPC-8C4FFF?style=flat-square&logo=amazonwebservices&logoColor=white) ![Amazon EC2](https://img.shields.io/badge/Amazon_EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white) ![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white) ![Certbot](https://img.shields.io/badge/Certbot-003A70?style=flat-square&logo=letsencrypt&logoColor=white) ![systemd](https://img.shields.io/badge/systemd-000000?style=flat-square&logoColor=white) |
| **CI/CD** | ![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white) ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white) ![pnpm](https://img.shields.io/badge/pnpm-F69220?style=flat-square&logo=pnpm&logoColor=white) ![Slack](https://img.shields.io/badge/Slack-4A154B?style=flat-square&logo=slack&logoColor=white) |
| **Testing & Quality** | ![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white) ![AssertJ](https://img.shields.io/badge/AssertJ-2C2255?style=flat-square&logoColor=white) ![Vitest](https://img.shields.io/badge/Vitest-6E9F18?style=flat-square&logo=vitest&logoColor=white) ![Testing Library](https://img.shields.io/badge/Testing_Library-E33332?style=flat-square&logo=testinglibrary&logoColor=white) ![ESLint](https://img.shields.io/badge/ESLint-4B32C3?style=flat-square&logo=eslint&logoColor=white) ![Prettier](https://img.shields.io/badge/Prettier-F7B93E?style=flat-square&logo=prettier&logoColor=black) |
<br>

## 서비스 아키텍처

![서비스 아키텍처](docs/images/architecture.png)

- AWS VPC 안에 퍼블릭 서브넷과 프라이빗 서브넷을 나눠 두었다. 프론트엔드(React SPA)와
  백엔드(Spring Boot)는 퍼블릭 서브넷의 같은 EC2 인스턴스에 배포되며, Nginx가 정적
  파일 서빙과 `/api/**` 리버스 프록시를 함께 담당한다.
- MySQL은 프라이빗 서브넷의 별도 EC2에서 실행되고, 애플리케이션 EC2에서만 접근할 수
  있어 외부에 직접 노출되지 않는다.
- 배포는 GitHub Actions가 빌드 산출물을 SSH로 서버에 전달하면, 서버의 `deploy.sh`가
  릴리스 디렉터리를 심볼릭 링크로 원자적으로 교체하고 헬스체크 실패 시 이전 버전으로
  자동 롤백한다.
- ESP32는 실시간 푸시 없이 0.3초 주기로 `GET /api/lockers/{lockerId}/lock-status`를
  폴링해 사물함 문 상태를 반영한다. 이 엔드포인트는 장치가 세션 인증 없이 호출할 수
  있도록 `/api/lockers/**` 전체가 인증에서 제외되어 있다.
- 인증 세션은 별도 세션 스토어(Redis 등) 없이 Spring 기본 인메모리 `HttpSession`으로
  관리된다.

## ERD

```mermaid
erDiagram
    MEMBER ||--o{ PRODUCT : "판매자로 등록"
    MEMBER ||--o{ TRANSACTION : "구매자로 결제"
    LOCKER ||--o{ PRODUCT : "지금까지 보관한 상품(이력)"
    PRODUCT ||--o| TRANSACTION : "거래 대상"
    PRODUCT ||--o{ PRODUCT_IMAGE : "사진"

    MEMBER {
        Long id PK
        String loginId UK
        String password
        String nickname
        MemberType memberType "REGISTERED(가입 회원) / GUEST(게스트)"
        LocalDateTime createdAt
    }

    LOCKER {
        Long id PK "실제 사물함 번호를 그대로 사용, 자동 증가 아님"
        LockStatus lockStatus "LOCKED(잠김) / UNLOCKED(열림)"
        UsageStatus usageStatus "AVAILABLE(비어있음) / RESERVED(예약됨) / OCCUPIED(보관중)"
    }

    PRODUCT {
        Long id PK
        Long lockerId FK "예약 시 채워짐, nullable(아직 미예약이면 null)"
        Long sellerMemberId FK
        String name
        Long price
        String description
        String sellerName "등록 시점 판매자 닉네임 스냅샷"
        ProductStatus status "PREPARING(등록됨)/RESERVED(예약됨)/SELLING(판매중)/SOLD(판매완료)/EXPIRED(판매만료)"
        LocalDateTime createdAt
        LocalDateTime reservedAt
        LocalDateTime reservationExpiresAt
        LocalDateTime depositStartedAt
        LocalDateTime sellingStartedAt
        LocalDateTime sellingExpiresAt
        LocalDateTime recoveryStartedAt
        LocalDateTime soldAt
    }

    PRODUCT_IMAGE {
        Long productId PK,FK "product_image 테이블, 별도 엔티티 아닌 값 타입 컬렉션"
        int imageOrder PK "등록 순서(productId와 합쳐 복합키)"
        String imageUrl
    }

    TRANSACTION {
        Long id PK
        Long productId FK
        Long lockerId "결제 시점 스냅샷, FK 제약 아님"
        Long buyerMemberId FK
        String buyerName "결제 시점 구매자 닉네임 스냅샷"
        Long price
        String paymentKey UK "토스 paymentKey 원본"
        String orderId "토스 orderId 원본"
        String approvedAt "토스 approvedAt 원본"
        TransactionStatus status "PAID(결제완료) / DONE(수령완료)"
        LocalDateTime createdAt
    }
```

- 모든 FK 표시는 실제 DB 외래키 제약이 아니라 애플리케이션에서만 참조하는 `Long` 값이다
  (`@ManyToOne` 미사용, 마이그레이션 도구 미도입).
- `Product.lockerId`는 판매자가 사물함을 예약하는 순간 채워진다. 판매기간이 만료돼
  판매자가 회수를 완료하면(`completeRecovery`) 다시 비워지지만, 구매자가 사서 수령까지
  끝난(`completePickup`) 물품은 상태만 SOLD로 남고 lockerId는 지워지지 않는다. 그래서
  같은 사물함에 여러 Product가 이력으로 쌓이며, 지금 실제로 그 사물함에 있는 상품인지는
  `Product`가 아니라 `Locker.usageStatus`로 따로 확인해야 한다.
- `Transaction.lockerId`는 사물함이 나중에 다른 상품으로 재사용돼도 그 거래가 어느
  사물함에서 이뤄졌는지 이력을 보존하기 위한 스냅샷 값이다.
- `Product.imageUrls`는 별도 엔티티가 아니라 `@ElementCollection`으로 매핑된
  `product_image` 컬렉션 테이블이다(사진 여러 장을 등록 순서대로 저장).

## 프로젝트 구조

```
.
├── backend/                       # Spring Boot API (Java 21 · Gradle)
│   ├── src/main/java/com/kirin/superservice/
│   │   ├── member/ · auth/         # 회원가입, 게스트 세션, 로그인
│   │   ├── locker/                 # 사물함 잠금·사용 상태, ESP32 폴링 API
│   │   ├── product/                # 물품 등록·예약·판매
│   │   ├── transaction/ · payment/ # 결제 승인, 수령 처리 (Toss Payments)
│   │   ├── image/ · health/        # 이미지 업로드, 헬스체크
│   │   └── global/                 # 공통 예외·인증·Slack 알림
│   ├── src/main/resources/        # application*.yaml, 초기 데이터(data.sql)
│   ├── src/test/java/             # 도메인별 단위·컨트롤러 테스트
│   └── deploy/                    # systemd 유닛, 배포 스크립트
├── frontend/                      # React SPA (Vite)
│   ├── src/routes/                # 화면 단위 라우트 (TanStack Router 파일 기반)
│   ├── src/components/{domain,layout,ui}/  # 도메인·레이아웃·shadcn 기반 공용 UI
│   ├── src/api/generated/         # OpenAPI(Orval) 자동 생성 API 클라이언트
│   ├── docs/RULESET.md, DESIGN.md # 프론트엔드 규칙, 디자인 토큰
│   └── deploy/                    # Nginx 설정, 배포 스크립트
├── docs/images/                   # README에 쓰는 다이어그램 이미지
└── .github/workflows/             # 백엔드/프론트엔드 CI·CD, PR 가드
```

## Known Issues

- **ESP32·관리자 페이지 무인증은 의도된 설계**: ESP32가 인증 없이 폴링할 수 있도록
  `/api/lockers/**` 전체(GET·PATCH)를 세션 인증에서 제외했고, `admin/lockers` 화면도
  별도 인가 없이 같은 공개 API를 그대로 쓴다. 지금은 의도한 동작이지만, 추후 보안을
  위해 정식 인가(판매자·구매자 본인 확인) 또는 장치 전용 인증(API 키 등) 도입이
  필요할 것으로 보인다.
- **세션 인메모리 저장**: 별도 세션 스토어(Redis 등) 없이 단일 인스턴스의 인메모리
  `HttpSession`을 사용해, 서버를 다중화하면 세션이 공유되지 않는다.
- **애플리케이션 서버 단일 인스턴스**: 프론트엔드·백엔드는 퍼블릭 서브넷의 EC2 한 대에
  함께 배포되어 있어 무중단 다중화나 오토스케일링이 되지 않는다.
- **ESP32 ↔ 서버 통신이 단방향 폴링**: 실시간 푸시(WebSocket 등) 대신 0.3초 주기 HTTP
  폴링 방식이라 네트워크 상태에 따라 반응 지연이 생길 수 있다. Wi-Fi 재연결이 일정
  시간(15초) 안에 되지 않으면 장치가 자동 재부팅하도록 완화 조치만 되어 있다.

## 개발자 조 구성원 정보

| 이름 | GitHub |
| --- | --- |
| Wongi Kim | [@cylin0201](https://github.com/cylin0201) |
| Jihyeong Hong | [@topograp2](https://github.com/topograp2) |
| Gibeom Lim | [@delphox60](https://github.com/delphox60) |
