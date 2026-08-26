import { cn } from "@/lib/utils";

/**
 * 홈 화면 상단 워드마크 (Figma "01 홈 · 사물함 현황"의 `Logo`, DESIGN.md §4.2).
 * 하위 화면에서는 이 자리를 `Header` 가 대신한다. 아이콘은 파비콘(`public/favicon.png`)과
 * 같은 마스코트 이미지라 별도 에셋 없이 그대로 재사용한다.
 */
export function Logo({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "flex h-[var(--header-height)] w-full items-center gap-1 px-5",
        className,
      )}
    >
      <img
        src="/favicon.png"
        alt=""
        className="size-10 shrink-0 object-cover"
      />
      <span className="font-brand text-2xl leading-7 font-bold text-[var(--color-text)]">
        오다가다
      </span>
    </div>
  );
}
