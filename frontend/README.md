# KIRIN FE

프런트엔드 프로젝트 골격. 기술 스택·폴더 구조·컨벤션은
[`docs/RULESET.md`](docs/RULESET.md), 디자인 토큰은 [`DESIGN.md`](DESIGN.md) 참조.

## 시작하기

```bash
pnpm install
cp .env.example .env
pnpm dev
```

개발 서버는 http://localhost:5173 에서 뜨며, `/api` 요청은
`vite.config.ts` 의 프록시를 통해 백엔드(기본 http://localhost:8080)로 전달된다.

## 주요 명령어

| 명령 | 설명 |
| --- | --- |
| `pnpm dev` | 개발 서버 |
| `pnpm build` | 타입체크 + 프로덕션 빌드 |
| `pnpm test` | 단위 테스트 (Vitest) |
| `pnpm lint` | ESLint |
| `pnpm format` | Prettier 포맷 |
| `pnpm gen:api` | `openapi.yaml` → `src/api/generated` 재생성 |
