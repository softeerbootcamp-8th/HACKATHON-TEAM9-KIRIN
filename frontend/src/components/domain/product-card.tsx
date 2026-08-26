import { MoreVertical } from "lucide-react";
import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";

type ProductCardBadge = {
  label: string;
  variant: "info" | "danger" | "muted" | "success" | "mine" | "mineSelling";
};

type ProductCardHighlight = {
  text: string;
  tone: "info" | "danger" | "mine" | "selling";
};

const HIGHLIGHT_TONE_COLOR: Record<ProductCardHighlight["tone"], string> = {
  info: "text-[var(--color-info)]",
  danger: "text-[var(--color-danger)]",
  // 남은 예약 시간 · 남은 점유 기간 강조 텍스트는 예약/판매 여부와 무관하게
  // 모두 primary 색을 쓴다 (Figma "03 홈 · 바텀시트(예약중·본인)", "04 홈 · 바텀시트(판매중·본인)")
  mine: "text-[var(--color-primary)]",
  selling: "text-[var(--color-primary)]",
};

type ProductCardProps = {
  name: string;
  price: number;
  /** 판매대기(PREPARING) 등 배지가 없는 상태도 있다 (Figma "07 내 리스트 · 판매 중") */
  badge?: ProductCardBadge;
  /** 진열함 위치/미지정 등 보조 정보 한 줄 (Figma 회색 캡션) */
  meta: string;
  /** 남은 예약 시간·남은 점유 기간 등 강조 캡션 (Figma "07 내 리스트 · 판매 중") */
  highlight?: ProductCardHighlight;
  thumbnailUrl?: string;
  onMoreClick?: () => void;
  className?: string;
};

/** 내 리스트 상품 카드 (Figma "ProductCard", DESIGN.md "07 내 리스트") */
export function ProductCard({
  name,
  price,
  badge,
  meta,
  highlight,
  thumbnailUrl,
  onMoreClick,
  className,
}: ProductCardProps) {
  return (
    <div
      className={cn(
        "flex w-full items-start gap-3 rounded-[10px] border border-[var(--color-border)] bg-[var(--color-bg)] p-3.5",
        className,
      )}
    >
      <div className="size-14 shrink-0 overflow-hidden rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-2)]">
        {thumbnailUrl && (
          <img src={thumbnailUrl} alt="" className="size-full object-cover" />
        )}
      </div>

      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex items-center gap-1.5">
          <p className="truncate text-[15px] font-semibold text-[var(--color-text-sub)]">
            {name}
          </p>
          {badge && (
            <Badge
              variant={badge.variant}
              className="shrink-0 px-1.5 py-0.5 text-[11px]"
            >
              {badge.label}
            </Badge>
          )}
        </div>
        <p className="tabular text-[15px] font-bold text-[var(--color-text-sub)]">
          {price.toLocaleString()}원
        </p>
        <p className="truncate text-xs text-[var(--color-text-muted)]">
          {meta}
        </p>
        {highlight && (
          <p
            className={cn(
              "text-xs font-medium",
              HIGHLIGHT_TONE_COLOR[highlight.tone],
            )}
          >
            {highlight.text}
          </p>
        )}
      </div>

      <button
        type="button"
        onClick={onMoreClick}
        aria-label="더보기"
        className="shrink-0 text-[var(--color-text-muted)]"
      >
        <MoreVertical className="size-6" />
      </button>
    </div>
  );
}
