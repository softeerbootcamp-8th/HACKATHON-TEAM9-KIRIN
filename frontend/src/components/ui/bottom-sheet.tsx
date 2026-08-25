import * as React from "react";
import { Drawer } from "vaul";
import { cn } from "@/lib/utils";

/**
 * 하단 고정 시트 (Figma `BottomSheet`, DESIGN.md §4.3 · §5.1).
 * vaul(Drawer) 기반 — 핸들바뿐 아니라 시트 어디를 잡고 아래로 슬라이딩해도
 * 닫히는 네이티브 바텀시트 제스처를 지원한다(스크롤 가능한 본문과는 vaul이
 * 자체적으로 구분해서 처리한다). 열림/닫힘 이동 애니메이션도 vaul이 드래그
 * 상태에 맞춰 직접 관리하므로 Content 쪽에 별도 slide 애니메이션 클래스를
 * 주지 않는다.
 */
const BottomSheet = Drawer.Root;
const BottomSheetTrigger = Drawer.Trigger;
const BottomSheetClose = Drawer.Close;

function BottomSheetOverlay({
  className,
  ...props
}: React.ComponentProps<typeof Drawer.Overlay>) {
  return (
    <Drawer.Overlay
      className={cn(
        "fixed inset-0 z-50 bg-black/40",
        "data-[state=open]:animate-in data-[state=open]:fade-in-0",
        "data-[state=closed]:animate-out data-[state=closed]:fade-out-0",
        className,
      )}
      {...props}
    />
  );
}

function BottomSheetContent({
  className,
  children,
  ...props
}: React.ComponentProps<typeof Drawer.Content>) {
  return (
    <Drawer.Portal>
      <BottomSheetOverlay />
      <Drawer.Content
        className={cn(
          "fixed inset-x-0 bottom-0 z-50 mx-auto flex max-h-[85dvh] w-full max-w-[var(--viewport-max)] flex-col",
          "rounded-t-[var(--radius-lg)] bg-[var(--color-bg)] pt-3 shadow-[var(--shadow-sheet)] outline-none",
          className,
        )}
        {...props}
      >
        {/* Sheet/HandleRow — 시각적 표시용. 드래그는 시트 전체에서 동작한다 */}
        <div
          className="flex h-1 w-full shrink-0 items-center justify-center"
          aria-hidden
        >
          <div className="h-1 w-9 rounded-full bg-[var(--color-border)]" />
        </div>
        {children}
      </Drawer.Content>
    </Drawer.Portal>
  );
}

function BottomSheetHeader({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      className={cn(
        "flex h-6 items-center justify-between px-4 pt-4",
        className,
      )}
      {...props}
    />
  );
}

function BottomSheetTitle({
  className,
  ...props
}: React.ComponentProps<typeof Drawer.Title>) {
  return (
    <Drawer.Title
      className={cn(
        "text-base font-semibold text-[var(--color-text)]",
        className,
      )}
      {...props}
    />
  );
}

function BottomSheetBody({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      className={cn("flex-1 overflow-y-auto px-4 pt-3 pb-6", className)}
      {...props}
    />
  );
}

export {
  BottomSheet,
  BottomSheetTrigger,
  BottomSheetClose,
  BottomSheetContent,
  BottomSheetHeader,
  BottomSheetTitle,
  BottomSheetBody,
};
