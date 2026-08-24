---
description: 지정한 커밋부터 현재까지의 변경사항으로 팀 컨벤션에 맞는 PR을 생성한다
argument-hint: <시작 커밋 SHA> [assignee 지정 시 함께 언급]
allowed-tools: Bash(gh pr:*), Bash(gh label:*), Bash(gh auth:*), Bash(git log:*), Bash(git diff:*), Bash(git show:*), Bash(git status:*), Bash(git branch:*), Bash(git rev-parse:*), Bash(git rev-list:*), Bash(git push:*), Read, Write, Grep, Glob
---

# PR 생성

시작 커밋: **$1**

`$1` 커밋을 **포함해서** 현재 HEAD까지의 변경사항으로 PR을 만든다.
`.github/PULL_REQUEST_TEMPLATE.md` 형식을 따른다.

## 1. 사전 확인

`gh auth status`로 인증을 확인한다. 실패하면 `gh auth login`을 안내하고 중단한다.

인자가 없으면 최근 커밋 목록(`git log --oneline -15`)을 보여주고
어느 커밋부터 묶을지 물어본다. 임의로 정하지 않는다.

`git rev-parse --verify $1^{commit}` 으로 SHA가 유효한지 확인한다.

## 2. 범위 계산

`$1`을 포함해야 하므로 범위는 `$1^..HEAD` 다.

    git log --oneline $1^..HEAD

`$1`이 최초 커밋이면 `$1^`가 없어 실패한다. 이때는 `--root`를 쓴다.

    git log --oneline --root HEAD

범위에 잡힌 커밋 목록을 사용자에게 먼저 보여주고, 의도한 범위가 맞는지
확인받은 뒤 진행한다. 범위를 잘못 잡으면 남의 커밋까지 PR에 들어간다.

## 3. 변경사항 파악

    git diff --stat $1^..HEAD
    git diff $1^..HEAD

커밋 메시지만 요약하지 말고 실제 diff를 읽는다. 커밋 메시지에 안 적힌
설계 판단이나 트레이드오프가 있으면 본문의 "리뷰어에게 하고 싶은 말"에 넣는다.

## 4. 관련 이슈 번호 찾기

순서대로 시도한다.

1. 현재 브랜치명에서 추출 — `backend/CLAUDE.md` 브랜치 규칙(`{이슈번호}/{이슈타입}/{이슈이름}`)상
   맨 앞 숫자가 이슈 번호다. 예: `7/feat/member-register-api` → `7`
2. 범위 내 커밋 메시지의 `#N` 참조
3. 못 찾으면 사용자에게 묻는다. 이슈 없이 진행해도 된다

## 5. 제목

커밋 메시지와 같은 형식: `[영역] 타입: 요약` (`backend/CLAUDE.md` 11장 기준)

- 영역: `BE` / `FE` / `ALL`
- 범위 내 커밋이 여러 영역에 걸치면 `ALL`
- 예: `[BE] feat: 회원 가입 API 구현`

## 6. 본문

`.github/PULL_REQUEST_TEMPLATE.md`의 4개 섹션을 채운다. 안내 주석(`<!-- ... -->`)은
지우고 실제 내용을 넣는다.

```markdown
## 📌 개요

{PR의 목적과 변경한 이유. 관련 이슈가 있으면 첫 줄에 `Closes #{번호}`}

## 🛠️ 작업 목록

- [x] {작업한 내용 1}
- [x] {작업한 내용 2}

## 🎬 동작 화면

{UI 변경이 있으면 변경 전/후 표. 백엔드 전용이거나 화면 변경이 없으면 "해당 없음"이라고 쓴다.
Swagger 응답이나 테스트 결과로 대체해도 된다.}

## 💬 리뷰어에게 하고 싶은 말

- {중점적으로 봐줬으면 하는 부분}
- {고민했던 지점과 선택한 이유 — diff만 봐서는 알 수 없는 "왜"}
- {검증하지 못한 부분, 남은 위험}
```

**작업 목록**은 커밋 제목 나열이 아니라 변경의 의미를 쓴다.

**리뷰어에게 하고 싶은 말**에는 검증하지 못한 부분과 남은 위험을 반드시 포함한다.

**글쓰기 톤** — 쉬운 말로 짧게 쓴다. 시맨틱·트레이드오프·원자성처럼
불필요하게 어려운 용어나 AI가 쓴 티 나는 표현은 피하고, 팀원이 한 번
읽고 바로 핵심을 파악할 수 있게 쓴다. 이미 리뷰로 해결된 내용을
장황하게 설명하지 않는다.

## 7. assignee · 리뷰어 · label

**assignee** — 사용자가 지정하지 않으면 본인으로 한다. `--assignee @me`

**리뷰어** — **자동으로 지정하지 않는다.** 이 저장소는 부트캠프 조직에
속해 있어 collaborator 목록에 팀원이 아닌 사람까지 포함될 수 있다. 잘못 지정하면
관계없는 사람에게 리뷰 요청이 간다. 사용자가 명시한 경우에만 지정한다.

**label** — 이 저장소에는 아직 `BE`/`FE`, `feat`/`fix` 같은 전용 label이 없고
GitHub 기본 label만 있다. 실행할 때 **반드시 실제 목록을 먼저 조회**한다.

    gh label list --limit 100 --json name,description

전용 label이 없으면 타입에 대략 대응하는 기본 label만 최선으로 매칭한다
(`feat`→`enhancement`, `fix`→`bug`, `docs`→`documentation`). 마땅한 게 없으면
label 없이 진행하고 알린다. **label을 새로 만들지 않는다.**

## 8. 푸시와 생성

브랜치가 원격에 없으면 먼저 푸시한다.

    git push -u origin HEAD

본문은 임시 파일에 쓰고 `--body-file`로 넘긴다. 인라인 `--body`는
따옴표 처리가 깨지기 쉽고, 금지 명령어 차단 hook의 오탐도 유발한다.

    gh pr create --base develop --title "{제목}" --body-file {임시파일} \
      --assignee @me --label "{label1},{label2}"

**base는 `develop`** 다 (`main ← develop ← 기능 브랜치`). `main`으로 열지 않는다.
현재 브랜치가 `develop`이나 `main`이면 PR을 만들 수 없으므로 중단하고 알린다.

label 지정이 실패해도 **PR 본체는 살린다.** 실패하면 PR을 먼저 만든 뒤
`gh pr edit`으로 재시도하고, 그래도 안 되면 사실대로 알린다.

## 9. 보고

PR URL과 함께 지정된 assignee·label을 알린다.
label을 못 붙였으면 이유를 알린다.

**리뷰어는 지정하지 않았음을 알리고, 직접 지정하도록 안내한다.**

    gh pr edit {번호} --add-reviewer {아이디},{아이디}
