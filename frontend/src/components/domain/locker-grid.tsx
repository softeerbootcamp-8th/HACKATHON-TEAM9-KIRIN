import { cn } from "@/lib/utils";
import { LockerCell, type LockerStatus } from "@/components/domain/locker-cell";

export type LockerGridItem = {
  number: number;
  status: LockerStatus;
  /** 본인 소유(reserved/selling)일 때만 쓰는 남은 시간/날짜 ("1:23", "D-3" 등) */
  detail?: string;
};

type LockerGridProps = {
  lockers: LockerGridItem[];
  onSelect?: (number: number) => void;
  className?: string;
};

/** 사물함 4열 그리드 (Figma "01 홈 · 사물함 현황", DESIGN.md §7) */
export function LockerGrid({ lockers, onSelect, className }: LockerGridProps) {
  return (
    <div className={cn("grid grid-cols-4 gap-2.5", className)}>
      {lockers.map((locker) => (
        <LockerCell
          key={locker.number}
          number={locker.number}
          status={locker.status}
          detail={locker.detail}
          onClick={onSelect ? () => onSelect(locker.number) : undefined}
        />
      ))}
    </div>
  );
}
