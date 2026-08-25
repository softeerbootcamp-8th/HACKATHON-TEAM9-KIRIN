import { ChevronLeft } from "lucide-react";
import { cn } from "@/lib/utils";

type HeaderProps = {
  /** 중앙 타이틀 */
  title: string;
  /** 지정하면 좌측 뒤로가기 아이콘 노출 (Figma "뒤로텍스트"/"뒤로텍스트수정") */
  onBack?: () => void;
  /** 지정하면 우측 텍스트 액션 노출 (Figma "Default"/"뒤로텍스트수정") */
  rightText?: string;
  onRightTextClick?: () => void;
  className?: string;
};

/**
 * 하위 화면 상단 내비게이션 (Figma `Header`, DESIGN.md §4.2 · §5.1).
 * 홈 화면은 이 컴포넌트 대신 `Logo` 를 사용한다.
 */
export function Header({
  title,
  onBack,
  rightText,
  onRightTextClick,
  className,
}: HeaderProps) {
  return (
    <header
      className={cn(
        "flex h-[var(--header-height)] w-full items-center bg-[var(--color-bg)]",
        className,
      )}
    >
      <div className="flex h-full w-14 shrink-0 items-center pl-4">
        {onBack && (
          <button
            type="button"
            onClick={onBack}
            aria-label="뒤로가기"
            className="flex size-6 items-center justify-center text-[var(--color-text)]"
          >
            <ChevronLeft className="size-6" />
          </button>
        )}
      </div>

      <p className="flex-1 truncate text-center text-[20px] font-semibold text-[var(--color-text)]">
        {title}
      </p>

      <div className="flex h-full w-14 shrink-0 items-center justify-end pr-4">
        {rightText && (
          <button
            type="button"
            onClick={onRightTextClick}
            className="text-sm text-[var(--color-text-muted)]"
          >
            {rightText}
          </button>
        )}
      </div>
    </header>
  );
}
