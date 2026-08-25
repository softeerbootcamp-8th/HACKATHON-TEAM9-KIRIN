import { cn } from "@/lib/utils";

/** 사물함 상태 (Figma "01 홈 · 사물함 현황", DESIGN.md §6) */
export type LockerStatus = "empty" | "reserved" | "selling";

const STATUS_LABEL: Record<LockerStatus, string> = {
  empty: "비어있음",
  reserved: "예약중",
  selling: "판매중",
};

const STATUS_STYLE: Record<LockerStatus, string> = {
  empty: "border-[var(--color-border)] bg-[var(--color-bg)]",
  reserved: "border-[var(--color-info)] bg-[var(--color-info-bg)]",
  selling: "border-[var(--color-danger)] bg-[var(--color-danger-bg)]",
};

const STATUS_LABEL_COLOR: Record<LockerStatus, string> = {
  empty: "text-[var(--color-text-muted)]",
  reserved: "text-[var(--color-info)]",
  selling: "text-[var(--color-danger)]",
};

type LockerCellProps = {
  number: number;
  status: LockerStatus;
  onClick?: () => void;
  className?: string;
};

/**
 * 사물함 현황 그리드 셀 (Figma "01 홈 · 사물함 현황" 참고 — 별도 컴포넌트로
 * 분리되어 있지 않아 실제 화면에서 역으로 추출했다. DESIGN.md §5.1 · §6).
 * 상태는 색만이 아니라 라벨 텍스트로도 함께 드러낸다.
 */
export function LockerCell({
  number,
  status,
  onClick,
  className,
}: LockerCellProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex h-[108px] w-full flex-col rounded-[var(--radius-sm)] border p-1.5 text-xs font-medium",
        STATUS_STYLE[status],
        className,
      )}
    >
      <span className="text-left text-[var(--color-text-muted)]">{number}</span>
      <span
        className={cn("mt-auto pb-2 text-center", STATUS_LABEL_COLOR[status])}
      >
        {STATUS_LABEL[status]}
      </span>
    </button>
  );
}
