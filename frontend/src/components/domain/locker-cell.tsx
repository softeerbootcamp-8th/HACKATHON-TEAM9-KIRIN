import { cn } from "@/lib/utils";

/**
 * 사물함 표시 상태 (Figma "01 홈 · 사물함 현황").
 * 실제 사용상태(usageStatus) + 본인 소유 여부(isMine)를 화면 표시용으로 합친 것.
 *
 * | 실제 상태 | 조건 | 표시 |
 * | --- | --- | --- |
 * | 비어있음 | - | empty |
 * | 예약됨/판매중 | 본인 소유 | reserved / selling |
 * | 예약됨/판매중 | 본인 소유 아님(비로그인 포함) | occupied |
 *
 * 타인의 예약/판매 정보는 구분 없이 전부 occupied(사용중)로 뭉뚱그려 표시한다.
 */
export type LockerStatus = "empty" | "reserved" | "selling" | "occupied";

const STATUS_LABEL: Record<LockerStatus, string> = {
  empty: "비어있음",
  reserved: "예약중",
  selling: "판매중",
  occupied: "사용중",
};

const STATUS_STYLE: Record<LockerStatus, string> = {
  empty: "border-[var(--color-primary)] bg-[var(--color-primary-weak)]",
  reserved: "border-[var(--color-mine)] bg-[var(--color-mine-reserved-bg)]",
  selling: "border-[var(--color-mine)] bg-[var(--color-mine-selling-bg)]",
  occupied: "border-[var(--color-border)] bg-[var(--color-disabled-bg)]",
};

const STATUS_LABEL_COLOR: Record<LockerStatus, string> = {
  empty: "text-[var(--color-primary)]",
  reserved: "text-[var(--color-mine)]",
  selling: "text-[var(--color-mine)]",
  occupied: "text-[var(--color-text-muted)]",
};

type LockerCellProps = {
  number: number;
  status: LockerStatus;
  /** 본인 소유(reserved/selling)일 때만 쓰는 남은 시간/날짜 ("1:23", "D-3" 등) */
  detail?: string;
  onClick?: () => void;
  className?: string;
};

/**
 * 사물함 현황 그리드 셀 (Figma "01 홈 · 사물함 현황" 참고 — 별도 컴포넌트로
 * 분리되어 있지 않아 실제 화면에서 역으로 추출했다. DESIGN.md §5.1 · §6).
 * 상태는 색만이 아니라 라벨 텍스트로도 함께 드러낸다. 본인 소유 사물함은
 * 라벨 아래에 남은 예약 시간(reserved) 또는 남은 판매 일수(selling)를 덧붙인다.
 */
export function LockerCell({
  number,
  status,
  detail,
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
        className={cn(
          "mt-auto flex flex-col items-center pb-2 text-center",
          STATUS_LABEL_COLOR[status],
        )}
      >
        <span>{STATUS_LABEL[status]}</span>
        {detail && <span>{detail}</span>}
      </span>
    </button>
  );
}
