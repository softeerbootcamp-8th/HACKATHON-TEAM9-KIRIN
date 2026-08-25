import * as React from "react";
import { cn } from "@/lib/utils";

/** shadcn/ui Textarea — `Input` 과 동일한 outline 스타일 (Figma "상품 설명"). */
function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "w-full min-w-0 resize-none rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-bg)] p-3.5 text-sm text-foreground",
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

export { Textarea };
