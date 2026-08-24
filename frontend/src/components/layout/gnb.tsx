import { Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";

/**
 * 상단 글로벌 내비게이션 — 로고 · 내비게이션 자리 · 로그인/회원가입.
 * 실제 메뉴 구성이 정해지면 이 컴포넌트를 프로젝트에 맞게 확장한다.
 */
export function Gnb() {
  return (
    <header
      className="sticky top-0 z-40 border-b border-border bg-[color-mix(in_srgb,var(--color-bg)_88%,transparent)] backdrop-blur"
      style={{ height: "var(--gnb-height)" }}
    >
      <div className="mx-auto flex h-full max-w-[var(--container-max)] items-center justify-between gap-3 px-4 md:gap-8 md:px-8">
        <Link to="/" className="flex items-center gap-2 text-lg font-bold">
          KIRIN
        </Link>

        <div className="flex items-center gap-2">
          <Button asChild variant="ghost" size="sm">
            <Link to="/">로그인</Link>
          </Button>
          <Button asChild size="sm">
            <Link to="/">회원가입</Link>
          </Button>
        </div>
      </div>
    </header>
  );
}
