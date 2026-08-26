import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1 rounded-[var(--radius-pill)] px-2.5 py-0.5 text-xs font-medium whitespace-nowrap",
  {
    variants: {
      variant: {
        neutral: "bg-[var(--color-surface-2)] text-[var(--color-text-sub)]",
        outline: "border border-border text-[var(--color-text-sub)]",
        live: "bg-[color-mix(in_srgb,var(--color-live)_18%,transparent)] text-[var(--color-live)]",
        warning:
          "bg-[color-mix(in_srgb,var(--color-warning)_18%,transparent)] text-[var(--color-warning)]",
        success:
          "bg-[color-mix(in_srgb,var(--color-success)_18%,transparent)] text-[var(--color-success)]",
        // 판매중 배지 등 — 배경을 통째로 채운다 (Figma `Badge/판매중`, "09 상품 상세")
        danger: "bg-[var(--color-selling)] text-white",
        // 예약중 진열함 등 — 배경을 통째로 채운다 (Figma "Badge/예약중")
        info: "bg-[var(--color-reserved)] text-white",
        // 본인 소유 예약중 배지 (Figma "Badge/예약중", "03 홈 · 바텀시트(예약중·본인)")
        mine: "bg-[var(--color-reserved)] text-white",
        // 본인 소유 판매중 배지 — 배경을 통째로 채운다 (Figma "Badge/판매중", "04 홈 · 바텀시트(판매중·본인)")
        mineSelling: "bg-[var(--color-selling)] text-white",
        // 판매완료 등 (Figma "07-2 내 리스트 · 거래 완료")
        muted: "bg-[var(--color-border)] text-[var(--color-text-muted)]",
      },
    },
    defaultVariants: { variant: "neutral" },
  },
);

function Badge({
  className,
  variant,
  ...props
}: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return (
    <span className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };
