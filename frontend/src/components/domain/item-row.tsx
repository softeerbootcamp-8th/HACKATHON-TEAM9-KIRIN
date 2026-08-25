import {
  AlertCircle,
  Check,
  CheckCircle2,
  Circle,
  Image,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";

type ItemRowStatus = "none" | "accept" | "cancel";

type ItemRowProps = {
  title: string;
  place: string;
  address: string;
  /** 썸네일 이미지 URL — 없으면 중립 placeholder 렌더 (Figma 원본 에셋 미확보) */
  thumbnailUrl?: string;
  /** 체크박스 선택 UI 노출 (Figma "Checkbox"/"CheckboxCheck") */
  selectable?: boolean;
  checked?: boolean;
  onCheckedChange?: (checked: boolean) => void;
  /** 수락/거절 처리 결과 표시 (Figma "Accept"/"Cancel") — selectable과 배타적 */
  status?: ItemRowStatus;
  /** 삭제 대상임을 알리는 우상단 경고 배지 (Figma "Delete") */
  deleteWarning?: boolean;
  onClick?: () => void;
  className?: string;
};

/**
 * 내 리스트 · 거래 요청 목록의 행 아이템 (Figma `ItemList`, DESIGN.md §5.1).
 * `selectable`/`status`/`deleteWarning` 은 서로 배타적으로 쓰인다.
 */
export function ItemRow({
  title,
  place,
  address,
  thumbnailUrl,
  selectable = false,
  checked = false,
  onCheckedChange,
  status = "none",
  deleteWarning = false,
  onClick,
  className,
}: ItemRowProps) {
  return (
    <div
      className={cn(
        "relative flex h-[72px] items-center gap-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-bg)] p-3",
        className,
      )}
      onClick={onClick}
    >
      {deleteWarning && (
        <span
          className="absolute -top-2 -right-1 flex size-4 items-center justify-center rounded-full bg-[var(--color-danger)] text-[var(--color-bg)]"
          aria-hidden
        >
          <AlertCircle className="size-3" />
        </span>
      )}

      {/* DogListItem 썸네일 자리 — 실제 상품 이미지 자산 미확보로 중립 placeholder 사용 */}
      <div className="flex size-10 shrink-0 items-center justify-center rounded-[var(--radius-sm)] bg-[var(--color-surface-2)] text-[var(--color-text-muted)]">
        {thumbnailUrl ? (
          <img
            src={thumbnailUrl}
            alt=""
            className="size-full rounded-[var(--radius-sm)] object-cover"
          />
        ) : (
          <Image className="size-4" />
        )}
      </div>

      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <p className="truncate text-[16px] text-[var(--color-text-sub)]">
          {title}
        </p>
        <p className="truncate text-xs text-[var(--color-text-muted)]">
          {place} · {address}
        </p>
      </div>

      {status === "accept" && (
        <span
          className="flex size-6 shrink-0 items-center justify-center rounded-full bg-[var(--color-success)] text-[var(--color-bg)]"
          aria-label="수락됨"
        >
          <Check className="size-3" />
        </span>
      )}

      {status === "cancel" && (
        <X
          className="size-6 shrink-0 text-[var(--color-danger)]"
          aria-label="거절됨"
        />
      )}

      {status === "none" && selectable && (
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onCheckedChange?.(!checked);
          }}
          aria-pressed={checked}
          aria-label="선택"
          className="shrink-0 text-[var(--color-primary)]"
        >
          {checked ? (
            <CheckCircle2 className="size-6" />
          ) : (
            <Circle className="size-6 text-[var(--color-border)]" />
          )}
        </button>
      )}
    </div>
  );
}
