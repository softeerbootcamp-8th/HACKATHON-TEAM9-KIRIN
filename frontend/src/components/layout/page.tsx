import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * 모바일 앱 셸 — 뷰포트 상한(`--viewport-max`) 안에서 중앙 정렬하고, 더 넓은
 * 화면에서는 좌우를 여백(레터박스) 처리한다 (DESIGN.md §4.1).
 *
 * `Logo`/`Header` 는 이 컨테이너 안에서 폭 전체를 채우도록 두고, 본문 섹션은
 * Figma 화면들과 동일하게 각자 좌우 패딩(보통 16px)을 직접 지정한다 — 이 컨테이너
 * 자체에는 기본 좌우 패딩을 넣지 않는다.
 */
export function PageContainer({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className="min-h-dvh w-full bg-[var(--color-surface-2)]">
      <div
        className={cn(
          "mx-auto flex min-h-dvh w-full max-w-[var(--viewport-max)] flex-col bg-[var(--color-bg)]",
          className,
        )}
      >
        {children}
      </div>
    </div>
  );
}
