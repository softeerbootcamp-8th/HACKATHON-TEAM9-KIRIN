# AGENTS.md

Codex 등 이 파일을 읽는 모든 AI 코딩 에이전트는 **이 저장소(backend)에서 코드를 생성·수정하기 전에 반드시 [`CLAUDE.md`](./CLAUDE.md)를 먼저 읽고 그 규칙을 따른다.**

- `CLAUDE.md`가 이 프로젝트의 코딩 컨벤션에 대한 단일 진실 공급원(source of truth)이다. 패키지 구조, 네이밍(Repository/Service, CRUD 메서드), DTO, 예외 처리, `Optional` 사용, 로깅 형식, 테스트 작성 양식, Git/PR 규칙이 모두 그 문서에 정의되어 있다.
- 도구(Claude, Codex 등)에 따라 규칙을 다르게 적용하지 않는다. `CLAUDE.md`의 내용은 도구와 무관하게 동일하게 적용된다.
- `CLAUDE.md`가 갱신되면(예: 도메인 용어 확정, 신규 의존성 도입) 이 파일을 다시 읽어 최신 규칙을 반영한다.
- 이 파일과 `CLAUDE.md`가 충돌하는 내용이 있다면 `CLAUDE.md`를 우선한다.
