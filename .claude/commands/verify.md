---
description: 변경된 영역의 테스트·빌드를 실행하고 완료 보고 형식으로 정리한다
argument-hint: [be | fe | all (선택, 기본은 자동 감지)]
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(./gradlew:*), Bash(cd *), Bash(git rev-parse:*), Bash(pnpm:*), Bash(npm:*), Read, Grep, Glob
---

# 검증

대상 지정: **$ARGUMENTS** (비어 있으면 자동 감지)

`backend/CLAUDE.md` 컨벤션을 따르는지 확인하며 검증 대상의 테스트·빌드를 실행한다.

## 1. 검증 대상 결정

인자가 있으면 그대로 따른다 (`be` / `fe` / `all`).
없으면 변경된 경로로 판단한다.

    git status --short
    git diff --name-only develop...HEAD

`backend/` 변경 → 백엔드, `frontend/` 변경 → 프론트, 양쪽 → 둘 다.
문서·설정만 바뀌었으면 실행할 검증이 없다고 알리고 끝낸다.

## 2. 저장소 루트 고정

**모든 경로는 저장소 루트 기준으로 만든다.** 상대경로를 쓰면 `backend/`나
`frontend/` 안에서 실행할 때 `backend/backend`를 찾아 실패한다.

    R=$(git rev-parse --show-toplevel)

이후 명령에서 `$R`를 앞에 붙인다.

## 3. 백엔드

    ( cd "$R/backend" && ./gradlew test )

Gradle Wrapper를 쓴다. 전역 `gradle`을 쓰지 않는다.

**`cd`는 반드시 서브셸 `( ... )` 안에서 한다.** 셸 작업 디렉터리는
호출 간에 유지되므로, 서브셸 없이 `cd`하면 이후 명령이 엉뚱한 위치에서 돈다.

## 4. 프론트엔드

먼저 `frontend/`에 실제 프로젝트가 있는지 확인한다.

    test -f "$R/frontend/package.json" && echo exists || echo none

`package.json`이 없으면 "프론트엔드 미착수 — 검증 대상 없음"이라고 보고하고 끝낸다.
있으면 패키지 매니저는 **pnpm 고정**이다 (`pnpm-lock.yaml` 존재, npm/yarn 혼용 금지).
`package.json`의 `scripts`에 정의된 명령을 그대로 사용한다. 정의되지 않은 명령을
임의로 추측해 실행하지 않는다.

    pnpm --dir "$R/frontend" lint
    pnpm --dir "$R/frontend" typecheck
    pnpm --dir "$R/frontend" build
    pnpm --dir "$R/frontend" test

## 5. 실패 처리

**실패를 숨기거나 우회하지 않는다.** 다음은 금지다.

- 실패하는 테스트 삭제·비활성화
- 검증 완화(assertion 약화, lint 규칙 끄기)
- 실패를 "일단 통과"로 보고

실패하면 출력 그대로 보여주고 원인을 분석한다. 고칠지는 사용자가 정한다.
한쪽이 실패해도 나머지 검증은 마저 실행한다. 부분 결과가 더 유용하다.

## 6. 보고

```markdown
## 검증 결과

| 항목 | 결과 |
|---|---|
| backend test | ✅ 통과 (N개) / ❌ 실패 (N개) |
| frontend lint | ✅ / ❌ (미착수면 "검증 대상 없음") |
| frontend typecheck | ✅ / ❌ |
| frontend build | ✅ / ❌ |
| frontend test | ✅ 통과 (N개) / ❌ 실패 (N개) |

## 변경 영역
- {건드린 영역과 파일}

## backend/CLAUDE.md 컨벤션 확인
- {네이밍(Repository/Service, CRUD), DTO record, Optional 사용, 로그 형식, 테스트 형식 중
  어긋난 부분이 있으면 파일:줄 단위로 짚는다. 없으면 "위반 없음"}

## 미검증 사항
- {실행하지 않은 검증과 그 이유}
- {남은 위험}
```

**미검증 사항을 비워두지 않는다.** 자동 검증으로 확인되지 않는 항목(통합 흐름,
동시성, 실제 DB 연동 등)은 명시한다.
