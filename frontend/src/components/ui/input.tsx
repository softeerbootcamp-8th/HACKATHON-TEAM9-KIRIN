import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * shadcn/ui Input — Figma "Input"(로그인/상품 등록) 스펙에 맞춘 outline 스타일.
 * 흰 배경 + 항상 보이는 테두리, placeholder는 별도 톤의 밝은 회색을 쓴다.
 */
function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "w-full min-w-0 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-bg)] p-3.5 text-sm text-foreground",
        "placeholder:text-[var(--color-text-placeholder)] outline-none transition-[box-shadow,border-color]",
        "focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40",
        "aria-invalid:border-destructive aria-invalid:ring-destructive/30",
        "disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
