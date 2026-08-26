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
        // 판매중 배지 등 (Figma `Badge/판매중`, DESIGN.md §3.1)
        danger: "bg-[var(--color-danger-bg)] text-[var(--color-danger)]",
        // 예약중 진열함 등 (Figma "Locker/reserved")
        info: "bg-[var(--color-info-bg)] text-[var(--color-info)]",
        // 본인 소유 예약중 배지 (Figma "Badge/예약중", "03 홈 · 바텀시트(예약중·본인)")
        mine: "bg-[var(--color-mine-reserved-bg)] text-[var(--color-mine)]",
        // 본인 소유 판매중 배지 — 배경을 통째로 채운다 (Figma "Badge/판매중", "04 홈 · 바텀시트(판매중·본인)")
        mineSelling: "bg-[var(--color-selling)] text-white",
        muted: "bg-[var(--color-surface-2)] text-[var(--color-text-muted)]",
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
