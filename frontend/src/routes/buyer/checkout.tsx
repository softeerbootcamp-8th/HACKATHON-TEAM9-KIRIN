import { useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { Circle, CircleDot } from "lucide-react";
import { cn } from "@/lib/utils";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { ItemRow } from "@/components/domain/item-row";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/buyer/checkout")({
  component: CheckoutPage,
});

// 구매 대상 상품 — 실제로는 상품 상세에서 넘어온 값(또는 조회 쿼리)으로 채운다.
const ORDER = {
  productName: "골프채",
  price: 600_000,
  location: "에테르노 청담 · 16번 사물함",
};
const SERVICE_FEE = 3_000;

const PAYMENT_METHODS = [
  { id: "kakaopay", label: "카카오페이" },
  { id: "card", label: "신용 · 체크카드" },
  { id: "transfer", label: "계좌이체" },
] as const;

/** Method/* — 결제 수단 선택 라디오 행 (Figma "10 결제") */
function PaymentMethodOption({
  label,
  selected,
  onSelect,
}: {
  label: string;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={cn(
        "flex w-full items-center gap-2.5 rounded-[var(--radius-sm)] border p-3.5 text-sm",
        selected
          ? "border-[var(--color-primary)] bg-[var(--color-primary-weak)] font-medium text-[var(--color-text-sub)]"
          : "border-[var(--color-border)] bg-[var(--color-bg)] text-[var(--color-text-muted)]",
      )}
    >
      {selected ? (
        <CircleDot className="size-5 text-[var(--color-primary)]" />
      ) : (
        <Circle className="size-5 text-[var(--color-border)]" />
      )}
      {label}
    </button>
  );
}

/** 결제 (Figma "10 결제") */
function CheckoutPage() {
  const router = useRouter();
  const [method, setMethod] =
    useState<(typeof PAYMENT_METHODS)[number]["id"]>("kakaopay");
  const total = ORDER.price + SERVICE_FEE;

  return (
    <PageContainer>
      <Header title="결제" onBack={() => router.history.back()} />

      <div className="flex flex-col gap-4 px-4 pt-2">
        <section className="flex flex-col gap-2">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            주문 상품
          </h2>
          <ItemRow
            title={ORDER.productName}
            place={`${ORDER.price.toLocaleString()}원`}
            address={ORDER.location}
          />
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            결제 수단
          </h2>
          {PAYMENT_METHODS.map((option) => (
            <PaymentMethodOption
              key={option.id}
              label={option.label}
              selected={method === option.id}
              onSelect={() => setMethod(option.id)}
            />
          ))}
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            결제 금액
          </h2>
          <div className="flex flex-col gap-2.5 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-2)] p-3.5 text-[13px]">
            <div className="flex items-center justify-between">
              <span className="text-[var(--color-text-muted)]">상품 금액</span>
              <span className="font-medium text-[var(--color-text-sub)]">
                {ORDER.price.toLocaleString()}원
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-[var(--color-text-muted)]">
                서비스 수수료
              </span>
              <span className="font-medium text-[var(--color-text-sub)]">
                {SERVICE_FEE.toLocaleString()}원
              </span>
            </div>
            <div className="h-px w-full bg-[var(--color-border)]" />
            <div className="flex items-center justify-between font-bold">
              <span className="text-sm text-[var(--color-text-sub)]">
                총 결제 금액
              </span>
              <span className="text-base text-[var(--color-primary)]">
                {total.toLocaleString()}원
              </span>
            </div>
          </div>
        </section>

        <p className="text-xs text-[var(--color-text-muted)]">
          결제 후에는 사물함이 즉시 열리며, 단순 변심에 의한 취소가 불가해요.
          주문 내용을 확인했으며 결제에 동의합니다.
        </p>
      </div>

      <div className="mt-auto flex flex-col border-t border-[var(--color-border)] px-4 py-3">
        <Button
          fullWidth
          size="lg"
          onClick={() => router.navigate({ to: "/buyer/checkout-complete" })}
        >
          {total.toLocaleString()}원 결제하기
        </Button>
      </div>
    </PageContainer>
  );
}
