import { cn } from "@/lib/utils";

/**
 * 홈 화면 상단 워드마크 (Figma `Logo`, DESIGN.md §4.2).
 * 하위 화면에서는 이 자리를 `Header` 가 대신한다.
 */
export function Logo({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "flex h-[var(--header-height)] w-full items-center px-5",
        className,
      )}
    >
      <span className="font-brand text-2xl leading-7 font-bold text-[var(--color-text)]">
        오다가다
      </span>
    </div>
  );
}
