이 문서는 Claude / Codex 등 AI 도구가 이 저장소(backend)의 코드를 생성·수정할 때 따라야 할 규칙이다.
아래 규칙은 팀 개발 컨벤션에서 도출되었으며, **모든 코드 생성 시 반드시 준수**한다.

> 이 프로젝트(`HACKATHON-TEAM9-KIRIN`)는 아직 도메인이 확정되지 않은 초기 단계다. 도메인이 정해지는 대로 "0. 도메인 용어" 표를 채우고, 실제로 도입한 기술(Flyway, Swagger 등)이 생기면 "기술 스택"과 "9. 기타 규칙"을 갱신한다.

## 기술 스택

- Java 21 / Spring Boot, Spring Web MVC, Spring Data JPA, Lombok
- DB: MySQL
- 로깅: SLF4J + logback (Spring Boot 기본 설정)
- 테스트: JUnit5 + AssertJ
- 협업: GitHub (Issue + PR)

아직 도입하지 않은 도구(Flyway, Swagger 등)는 실제로 `build.gradle`에 추가된 뒤에만 이 문서에 규칙으로 반영한다. 도입되지 않은 도구를 코드에서 임의로 가정하지 않는다.

---

## 0. 도메인 용어 (Ubiquitous Language)

코드에서 도메인을 가리킬 때는 아래 표의 용어를 **그대로** 클래스명/필드명/메서드명에 반영한다. 새 도메인 개념이 코드에 등장하면 **먼저 이 표에 추가**하고 나서 코드에 반영한다. 한글 용어는 커밋 메시지·문서·로그 메시지에, 괄호 안 영문 용어는 코드 식별자에 사용한다.

| 용어 | 정의 |
| --- | --- |
| 물품보관함(Locker) | 판매자가 물품을 넣고 잠그면, 구매자가 결제 후 문을 열어 수령하는 물리적 보관함 1개 단위. 고유 식별자(lockerId)를 가진다. |
| 잠금상태(LockStatus) | 물품보관함 문의 개폐 상태. `LOCKED`(잠김) / `UNLOCKED`(열림) 두 값을 가진다. ESP32가 이 값을 폴링해 서보모터를 움직인다. |
| 사용상태(UsageStatus) | 물품보관함에 판매할 물품이 들어 있는지 여부. `AVAILABLE`(비어 있음) / `OCCUPIED`(사용 중) 두 값을 가진다. 잠금상태와 별개다. |
| 물품(Product) | 판매자가 물품보관함에 넣어 파는 중고 물품 1건. |
| 물품상태(ProductStatus) | 물품의 판매 진행 상태. `PREPARING`(등록됨, 보관함에 넣기 전) / `SELLING`(판매중) / `SOLD`(판매완료) 세 값을 가진다. |
| 판매자명(sellerName) | 물품을 등록한 사람의 표시용 이름. 회원 기능이 없어 계정과 연결되지 않는다. |
| 결제(Payment) | 구매자가 토스페이먼츠로 물품보관함 이용 대금을 지불하는 행위/내역. 토스의 `paymentKey`, `orderId`, `amount`를 그대로 식별자·필드명으로 사용한다. |
| 거래(Transaction) | 구매자가 물품 하나를 결제하고 수령하기까지의 1건. 결제가 승인된 뒤에만 만들어지며 토스의 `paymentKey`, `orderId`, `approvedAt`을 그대로 보관한다. |
| 거래상태(TransactionStatus) | 거래의 진행 상태. `PAID`(결제완료, 수령 전) / `DONE`(수령완료) 두 값을 가진다. |
| 구매(Purchase) | 구매자가 물품 대금을 결제하고 거래를 성립시키는 행위. |
| 구매자명(buyerName) | 물품을 구매한 사람의 표시용 이름. 회원 기능이 없어 계정과 연결되지 않는다. |
| 회원(Member) | 서비스에 가입한 사용자. 로그인 아이디(loginId)와 비밀번호(password)로 인증한다. |
| 인증(Auth) | 로그인/로그아웃 처리. 로그인에 성공하면 세션을 발급하고, 로그아웃하면 세션을 무효화한다. |

### 코드 반영 예시

- 용어 표에 없는 수식어를 필드/클래스명에 붙이지 않는다 (예: 표에 `Grade`만 있는데 `MemberGrade`처럼 임의로 접두어를 붙이지 않는다).
- 금액 관련 필드는 `~Price` 접미사로 통일한다 (예: `startingPrice`, `currentPrice`).
- 아직 표에 없는 개념을 구현해야 한다면, 먼저 팀과 논의해 한글/영문 용어를 표에 추가한 뒤 그 영문 용어를 클래스/필드명으로 그대로 사용한다.

---

## 1. 패키지 구조

도메인 단위로 패키지를 구성하고, 공통 관심사는 `global`에 둔다.

```
com.kirin.superservice
 ├─ {도메인}
 │   ├─ controller      // 요청/응답 처리, DTO 주고받음
 │   ├─ dto
 │   │   ├─ request
 │   │   └─ response
 │   ├─ service         // 비즈니스 로직 (도메인 관점 행위)
 │   ├─ repository
 │   │   ├─ {도메인}Repository            // 인터페이스
 │   │   └─ {도메인}{조회방식}Repository   // 구현체 (예: MemberJpaRepository)
 │   └─ domain          // 핵심 도메인 모델
 └─ global
     ├─ exception
     └─ ...
```

---

## 2. Repository vs Service 네이밍

**Repository는 영속성 관점, Service는 비즈니스 행위 관점**으로 이름을 짓는다.

- Repository 인터페이스: `{도메인}Repository`
- Repository 구현체: 조회 방식을 접두사로 (`MemberJpaRepository`, `MemberMemoryRepository`)
- Repository 메서드: `save`, `findById` 같은 영속성 네이밍
- Service 메서드: `registerMember`, `placeOrder` 같은 행위 네이밍 (영속성 이름을 그대로 노출하지 않음)

```java
// Repository
public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(Long id);
}

// Service
public Member registerMember(RegisterMemberCommand command) { ... }
```

---

## 3. CRUD 메서드 네이밍 (엄격 준수)

`동작 + 대상/조건` 순서로 작성한다. 조회 조건은 `By` 뒤에 붙인다.

| 목적 | 규칙 | 예시 |
| --- | --- | --- |
| 저장/변경 반영 | `save` | `save(member)` |
| 신규 삽입만 | `insert` / `create` | `insert(order)` |
| 선택적 단건 조회 | `findBy...` → **Optional 반환** | `findById(id)` |
| 필수 단건 조회 | `get...` → **없으면 예외** | `getMember(id)` |
| 다건 조회 | `findAllBy...` → **빈 컬렉션 반환** | `findAllByStatus(status)` |
| 존재 확인 | `existsBy...` | `existsByLoginId(loginId)` |
| 개수 조회 | `countBy...` | `countByStatus(status)` |
| 특정 필드 수정 | `update...By...` → `int` 반환 | `updateStatusById(id, status)` |
| 삭제 | `deleteBy...` | `deleteById(id)` |
| 정렬 | `OrderBy필드Asc/Desc` | `...OrderByCreatedAtDesc` |
| 제한 조회 | `findFirst`, `findTopN` | `findTop10By...` |
| 락 조회 | `findByIdForUpdate` | `findByIdForUpdate(id)` |
| 연관 조회 | `With...` (Fetch Join) | `findByIdWithMember(id)` |

### find vs get

```
find : 결과가 없을 수 있음 → Optional 반환 (주로 Repository)
get  : 반드시 존재해야 함 → 없으면 예외 (주로 Service 계층에서 구현)
```

```java
Optional<Member> findById(Long id);                 // Repository

public Member getById(Long id) {                    // Service
    return memberRepository.findById(id)
            .orElseThrow(MemberNotFoundException::new);
}
```

### 조건 조합 / 정렬

- 조합: `And`, `Or`, `In`/`NotIn`, `Between`, `LessThan(Equal)`, `GreaterThan(Equal)`, `IsNull`/`IsNotNull`, `Like`/`Containing`
- 정렬: `OrderBy필드Asc/Desc`, **동적 정렬은 메서드명에 넣지 말고 `Sort`/`Pageable`로 전달**

### 소프트 삭제

실제 삭제가 아니라 상태 변경이면 `delete`를 쓰지 않고 비즈니스 상태 동사를 쓴다.

```
deactivate(비활성화), archive(보관), withdraw(탈퇴), cancel(취소), close(종료)
```

### 금지 네이밍

```
select, load, search(단건), read, remove, process, handle → 사용 금지
```

- `search`는 **여러 조건을 동적 조합하는 검색**에 한해 허용 (`searchMembers(condition, pageable)`)

---

## 4. DTO

- **`record`로만** 작성한다.
- `request` / `response` 패키지를 분리한다.
- 클래스명은 `~Request`, `~Response`로 끝낸다.
- 도메인 변환 로직은 DTO 내부에 둔다 (`fromEntity`).

```java
public record MemberResponse(Long id, String nickname) {
    public static MemberResponse fromEntity(Member member) {
        return new MemberResponse(member.getId(), member.getNickname());
    }
}
```

---

## 5. 예외 처리

예외는 `enum`으로 관리하며 `statusCode` / `식별 코드` / `message`를 갖는다.

```java
public enum ErrorCode {
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    INVALID_REQUEST(400, "INVALID_REQUEST", "요청 값이 올바르지 않습니다.");
    // statusCode, code, message 필드
}
```

식별 코드는 `MEMBER_NOT_FOUND`처럼 **코드만 보고도 예외를 파악**할 수 있게 짓는다.

---

## 6. Optional 사용 규칙

`Optional`은 **반환값이 없을 수 있음을 호출자에게 알리는 도구**다.

- ✅ 메서드 반환 타입으로만 사용한다.
- ❌ 클래스 필드, 메서드 매개변수에 사용하지 않는다.
- 값이 없을 때: `.orElseThrow(() -> new XxxException(...))`
- 비용 있는 기본값(객체 생성, DB 조회)은 `orElse()`가 아니라 **`orElseGet()`** 사용.

```java
Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new MemberNotFoundException(memberId));

Member member = optionalMember.orElseGet(() -> createDefaultMember());
```

---

## 7. 로그 작성 양식

로그 한 줄로 **언제 / 어디서 / 무엇을 / 누구에 대해** 를 알 수 있어야 한다.

### 규칙

- SLF4J **파라미터 바인딩(`{}`)** 사용. 문자열 결합(`+`) 금지.
- 메시지 형식: `{한글 서술} - {key}={value}, {key}={value}`
- 엔티티 관련 로그에는 **식별자(ID) 반드시 포함**.
- 요청 추적을 위해 MDC에 `requestId`(요청 진입 시 UUID 생성, 응답 종료 시 `clear()`)를 담는다.
- 민감정보(비밀번호 평문, 토큰 전체, 주민번호)는 **로그에 남기지 않고**, 이메일·전화번호 등은 마스킹한다.

### logback 패턴

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%X{requestId}] [%thread] %logger{36} - %msg%n
```

```
2026-08-24 14:03:11.482 [INFO ] [a1b2c3d4] [http-nio-8080-exec-2] c.k.superservice.member.MemberService - 회원 가입 완료 - memberId=1024
```

### 레벨 기준

- `debug`: 개발 환경 전용. **상용에 남기지 않는다.**
- `info`: 운영 로직상 알려야 하는 상태 변화 (가입 완료, 주문 생성 등).
- `warn`: 실패는 아니나 주의가 필요한 상황 (재시도, 폴백).
- `error`: 예외를 `catch`하여 처리한 경우.

### 예외 로깅

throwable은 **마지막 인자**로 넘긴다.

```java
// 권장
log.error("회원 가입 처리 실패 - memberId={}", memberId, e);

// 금지
log.error("에러: " + e.getMessage());   // 스택 트레이스 유실
log.error("실패 {}", e);                 // throwable을 파라미터로 소비
e.printStackTrace();
```

### 금지

- 반복문 내부 info 이상 로그
- `System.out.println`
- 빈 `catch` 블록 (로그 없이 예외 무시)

---

## 8. 테스트 작성 양식

- **JUnit5 + AssertJ**로 한정한다.
- 메서드명은 **한글 동사형(`~다`)**, 형식은 `{조건}면_{기대결과}다`.
- 본문은 `// given`, `// when`, `// then` 3단 구조로 명시한다.
- 한 테스트는 하나의 검증 목적만 다룬다.
- 예외 검증 등 실행과 단언이 붙으면 `// when & then`으로 결합한다.
- 케이스가 많으면 `@Nested` + `@DisplayName`으로 그룹핑한다.

```java
@Test
void 유효한_회원정보로_가입하면_회원이_저장된다() {
    // given
    RegisterMemberCommand command = new RegisterMemberCommand("loginId", "nickname");

    // when
    Member savedMember = memberService.registerMember(command);

    // then
    assertThat(savedMember.getId()).isNotNull();
    assertThat(savedMember.getNickname()).isEqualTo("nickname");
}

@Test
void 존재하지_않는_회원을_조회하면_예외가_발생한다() {
    // given
    Long notExistId = 999L;

    // when & then
    assertThatThrownBy(() -> memberService.getById(notExistId))
            .isInstanceOf(MemberNotFoundException.class);
}
```

- 예외: `assertThatThrownBy(...).isInstanceOf(...)`
- JUnit 기본 단언(`assertEquals` 등) 사용 금지 → AssertJ로 통일.

---

## 9. 기타 규칙

- **환경 변수**: `application.yaml`(공통) / `application-dev.yaml` / `application-prod.yaml`로 분리.
- **DB 형상 관리**: 별도 마이그레이션 도구(Flyway 등)를 아직 도입하지 않았다. 도입 전까지는 엔티티 변경 시 팀에 공유하고, 도구를 도입하면 이 항목을 갱신한다.
- **API 문서화**: Swagger 등을 아직 도입하지 않았다. 도입 시 설정을 별도 클래스로 분리해 다른 로직과 구분한다.
- **식별자(ID) 타입**: `bigint` (`Long`).

---

## 10. 위험 명령어

다음 명령은 `.claude/hooks/block-forbidden-commands.sh`(PreToolUse hook)가 Bash 실행 단계에서 차단한다.
차단은 1차 방어선이며, 최종 방어는 사람의 확인이다.

- `git clean`, `git push --force`(`--force-with-lease`는 허용), `git reset --hard`
- `docker compose down -v`
- `flyway clean`
- `DROP`/`TRUNCATE` (mysql 등 DB 클라이언트 호출 시)

위 작업이 꼭 필요하면 팀 확인 후 **사람이 직접** 실행한다.

---

## 11. Git / PR

### 브랜치

```
main    : 운영 배포
develop : 개발 통합
{이슈번호}/{이슈타입}/{이슈이름}   예: 7/feat/login-api
```

이슈타입 = 커밋 타입: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`

### PR 제목

영역 = `BE` / `FE` / `EMBEDDED`(esp32/ 등 임베디드 펌웨어) / `ALL`(여러 영역 공통)

```
[BE] {type}: {message}
예: [BE] feat: 회원 가입 API 구현
```

### 머지

- 팀 컨벤션에 맞는 코드 리뷰 후 머지.
- 머지 방식은 **Squash Merge**.

---

## 요약 체크리스트 (코드 생성 전 확인)

- [ ] 클래스/필드/메서드명이 도메인 용어(Ubiquitous Language) 표와 일치하는가 (표에 없는 개념이면 먼저 표에 추가했는가)
- [ ] Repository 메서드는 영속성 네이밍, Service는 행위 네이밍인가
- [ ] `find`(Optional) / `get`(예외) 구분이 정확한가
- [ ] 금지 네이밍(select, load, remove, process...)을 쓰지 않았는가
- [ ] DTO는 `record`이고 `~Request`/`~Response`로 끝나는가
- [ ] `Optional`을 필드/매개변수에 쓰지 않았는가
- [ ] 로그가 `{}` 바인딩 + `key=value` 형식이고 throwable을 마지막 인자로 넘겼는가
- [ ] 테스트 메서드명이 한글 `~다`이고 given/when/then 구조인가
- [ ] 식별자 타입이 `Long`(bigint)인가
- [ ] 아직 도입하지 않은 도구(Flyway, Swagger 등)를 임의로 가정하지 않았는가
- [ ] 위험 명령어(`git clean`, `git push --force`, `git reset --hard`, `docker compose down -v`, `flyway clean`, `DROP`/`TRUNCATE`)를 실행하려 하지 않았는가
