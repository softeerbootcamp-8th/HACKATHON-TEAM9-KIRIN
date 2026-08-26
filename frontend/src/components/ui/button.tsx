import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

/**
 * shadcn/ui Button (new-york). variant 색은 globals.css 의 시맨틱 토큰을 따른다.
 * `default`/`secondary`/disabled 상태 색은 Figma `Button` 컴포넌트
 * (primary/secondary/disabled, DESIGN.md §5.1) 스펙을 그대로 따른다.
 */
const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-[var(--radius-sm)] text-sm font-semibold transition-all disabled:pointer-events-none outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0",
  {
    variants: {
      variant: {
        default:
          "bg-primary text-primary-foreground hover:bg-[var(--color-primary-hover)] disabled:bg-[var(--color-disabled-bg)] disabled:text-[var(--color-disabled-text)]",
        secondary:
          "border border-[var(--color-border-strong)] bg-transparent text-foreground hover:bg-[var(--color-surface-2)] disabled:border-[var(--color-border)] disabled:text-[var(--color-disabled-text)]",
        // 테두리·글자 모두 primary 색인 아웃라인 버튼 (Figma "05 사물함 예약 · 상품 선택"의 "자리 예약")
        outline:
          "border border-[var(--color-primary)] bg-transparent text-[var(--color-primary)] hover:bg-[var(--color-primary-weak)] disabled:border-[var(--color-disabled-bg)] disabled:text-[var(--color-disabled-text)]",
        ghost:
          "bg-transparent hover:bg-[var(--color-surface-2)] disabled:opacity-50",
        destructive:
          "bg-destructive text-destructive-foreground hover:opacity-90 disabled:opacity-50",
        link: "text-primary underline-offset-4 hover:underline disabled:opacity-50",
      },
      size: {
        default: "h-10 px-5 py-2",
        sm: "h-9 px-4",
        lg: "h-12 px-8 text-base", // Figma CTA 버튼 높이(48px)
        icon: "size-10",
      },
      fullWidth: {
        true: "w-full",
        false: "",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
      fullWidth: false,
    },
  },
);

function Button({
  className,
  variant,
  size,
  fullWidth,
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
  }) {
  const Comp = asChild ? Slot : "button";
  return (
    <Comp
      data-slot="button"
      className={cn(buttonVariants({ variant, size, fullWidth, className }))}
      {...props}
    />
  );
}

export { Button, buttonVariants };
