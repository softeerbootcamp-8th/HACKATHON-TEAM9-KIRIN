# KIRIN · 디자인 가이드

> 프런트엔드 디자인 시스템 골격 문서. 화면 명세·도메인 용어는 기획이 정해지면
> 이 문서에 채운다. 아래 값(색·타이포·간격 등)은 임시 기본값이며, 실제
> 디자인이 나오면 `src/styles/globals.css` 와 함께 갱신한다.

---

## 목차

1. [개요](#1-개요)
2. [디자인 원칙](#2-디자인-원칙)
3. [디자인 토큰](#3-디자인-토큰)
4. [레이아웃 · 내비게이션](#4-레이아웃--내비게이션)
5. [컴포넌트](#5-컴포넌트)
6. [상태 · 인터랙션 규칙](#6-상태--인터랙션-규칙)
7. [화면 명세](#7-화면-명세)
8. [도메인 용어](#8-도메인-용어)

---

## 1. 개요

| 항목 | 내용 |
|------|------|
| 서비스명 | KIRIN |
| 플랫폼 | (TBD — PC 웹 / 반응형 / 모바일) |
| 테마 | (TBD — 라이트 / 다크 / 둘 다) |

> 이 표는 기획이 확정되는 대로 채운다.

---

## 2. 디자인 원칙

> 프로젝트의 핵심 가치를 뒷받침하는 3~5개 원칙을 여기에 정의한다.
> (예: 정보 가독성, 신뢰 요소 노출, 비가역 액션 확인, 저부하 화면 등)

1. ―
2. ―
3. ―

---

## 3. 디자인 토큰

`src/styles/globals.css` 가 단일 출처(SSOT)다. 아래는 현재 기본값 스냅샷이며,
실제 값은 항상 코드를 기준으로 한다.

### 3.1 색상

```css
:root {
  --color-bg: #ffffff;
  --color-surface: #ffffff;
  --color-surface-2: #f5f6f8;
  --color-border: #e2e5ea;

  --color-text: #16181d;
  --color-text-sub: #5b6270;
  --color-text-muted: #8b93a1;

  --color-primary: #4c82fb;

  --color-success: #1a9e5c;
  --color-warning: #b3760a;
  --color-danger: #d13c3c;
}
```

### 3.2 타이포그래피

```css
--font-sans: system-ui, -apple-system, "Apple SD Gothic Neo", "Noto Sans KR", sans-serif;
--font-num: ui-monospace, var(--font-sans);
```

숫자(금액·시간 등)를 나열할 때는 `.tabular`(tabular-nums)를 적용해 자릿수가
흔들리지 않게 한다.

### 3.3 간격 · 반경

```css
--space-1: 4px;  --space-2: 8px;  --space-3: 12px; --space-4: 16px;
--space-5: 20px; --space-6: 24px; --space-8: 32px; --space-10: 40px;

--radius-sm: 8px;   /* 배지 · 입력 필드 */
--radius-md: 12px;  /* 버튼 · 카드 */
--radius-lg: 16px;  /* 패널 · 모달 */
--radius-pill: 999px;

--container-max: 1200px;
--gnb-height: 64px;
```

---

## 4. 레이아웃 · 내비게이션

### 4.1 GNB (상단 글로벌 내비게이션)

- 높이 `--gnb-height`, 좌측 로고, 우측 내비게이션(`src/components/layout/gnb.tsx`).
- 실제 메뉴 구성·역할 구분이 정해지면 이 섹션과 컴포넌트를 함께 갱신한다.

### 4.2 콘텐츠 영역

- 최대 폭 `--container-max` 중앙 정렬(`PageContainer`, `src/components/layout/page.tsx`).

---

## 5. 컴포넌트

`src/components/ui`(shadcn/ui 프리미티브)로 시작한다. 새 화면에 필요한
프리미티브는 CLI로 추가한다: `pnpm dlx shadcn@latest add <name>`.

도메인 컴포넌트(카드, 리스트, 배지 등)는 화면이 정해지는 대로
`src/components/domain` 에 추가하고 이 섹션에 명세를 채운다.

| 컴포넌트 | 스타일 | 용도 |
|------|--------|------|
| ― | ― | ― |

---

## 6. 상태 · 인터랙션 규칙

> 버튼 활성 조건, 필수 입력 검증, 비가역 액션 확인 등 화면 전반에 적용되는
> 규칙을 여기에 정의한다.

- ―

---

## 7. 화면 명세

> 화면(라우트) 단위로 목적·구성·전환 조건을 정리한다. 기획이 확정되면
> 화면별로 섹션을 추가한다.

### `/` · 홈

- 임시 placeholder (`src/routes/index.tsx`). 실제 화면 요구사항이 정해지면
  교체한다.

---

## 8. 도메인 용어

> 서비스 고유의 용어·약어를 여기에 정리한다.

| 용어 | 설명 |
|------|------|
| ― | ― |

---

*이 문서는 골격(스켈레톤)입니다. 화면·색·간격 등 실제 값이 정해지면 이 문서와
`src/styles/globals.css` 를 함께 갱신하세요.*
