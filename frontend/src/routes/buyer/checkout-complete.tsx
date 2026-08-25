import { createFileRoute, useRouter } from "@tanstack/react-router";
import { PageContainer } from "@/components/layout/page";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/buyer/checkout-complete")({
  component: CheckoutCompletePage,
});

// 실제로는 방금 결제한 주문 정보를 그대로 받아와 채운다.
const ORDER = {
  orderNumber: "20260825-004821",
  productName: "골프채",
  paymentMethod: "카카오페이",
  amount: 603_000,
  lockerNumber: 16,
};

/**
 * 결제 완료 · 사물함 열림 (Figma "11 결제 완료 · 사물함 열림").
 * 뒤로가기가 없는 종료 화면 — "수령 완료"를 누르면 다시 스캔 화면으로 돌아간다.
 */
function CheckoutCompletePage() {
  const router = useRouter();

  return (
    <PageContainer>
      <div className="flex flex-col items-center gap-5 px-4 pt-[70px] text-center">
        <div className="flex h-[100px] w-[100px] shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)]">
          <span className="text-[44px] font-bold text-white">✓</span>
        </div>

        <div className="flex flex-col gap-1.5">
          <h1 className="text-[22px] font-bold text-[var(--color-text)]">
            결제가 완료됐어요
          </h1>
          <p className="text-sm text-[var(--color-text-muted)]">
            {ORDER.lockerNumber}번 사물함이 열렸어요.
            <br />
            상품을 꺼내고 문을 닫으면 자동으로 잠겨요.
          </p>
        </div>

        <div className="flex w-full flex-col gap-2.5 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-2)] p-3.5 text-left text-[13px]">
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">주문번호</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {ORDER.orderNumber}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">상품</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {ORDER.productName}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">결제 수단</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {ORDER.paymentMethod}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">결제 금액</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {ORDER.amount.toLocaleString()}원
            </span>
          </div>
        </div>
      </div>

      <div className="mt-auto flex flex-col border-t border-[var(--color-border)] px-4 py-3">
        <Button
          fullWidth
          size="lg"
          onClick={() => router.navigate({ to: "/buyer/scan" })}
        >
          수령 완료
        </Button>
      </div>
    </PageContainer>
  );
}
