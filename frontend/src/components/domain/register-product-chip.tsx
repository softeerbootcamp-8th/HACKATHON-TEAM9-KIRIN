import { Link } from "@tanstack/react-router";
import { cn } from "@/lib/utils";

/** "+ 상품 등록" 바로가기 칩 (Figma "Button/상품 등록") */
export function RegisterProductChip({ className }: { className?: string }) {
  return (
    <Link
      to="/seller/products/new"
      className={cn(
        "self-end rounded-[var(--radius-pill)] border border-[var(--color-primary)] px-3 py-1.5 text-xs font-medium text-[var(--color-primary)]",
        className,
      )}
    >
      + 상품 등록
    </Link>
  );
}
