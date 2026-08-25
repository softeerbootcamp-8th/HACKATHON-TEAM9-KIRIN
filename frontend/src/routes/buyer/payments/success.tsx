import { useEffect, useRef, useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { Button } from "@/components/ui/button";
import { usePurchaseProduct } from "@/api/generated/transactions/transactions";

type PaymentSuccessSearch = {
  productId: number;
  paymentKey: string;
  orderId: string;
  amount: number;
};

export const Route = createFileRoute("/buyer/payments/success")({
  validateSearch: (search: Record<string, unknown>): PaymentSuccessSearch => ({
    productId: Number(search.productId),
    paymentKey: String(search.paymentKey ?? ""),
    orderId: String(search.orderId ?? ""),
    amount: Number(search.amount),
  }),
  component: PaymentSuccessPage,
});

/**
 * 토스 결제창이 성공 시 리다이렉트하는 화면. 여기서 실제 거래를
 * 생성(`POST /transactions` = 결제 승인 겸 거래 생성)한 뒤 완료 화면으로 넘어간다.
 */
function PaymentSuccessPage() {
  const router = useRouter();
  const { productId, paymentKey, orderId, amount } = Route.useSearch();
  const purchaseProduct = usePurchaseProduct();
  const [failed, setFailed] = useState(false);
  const requested = useRef(false);

  useEffect(() => {
    if (requested.current) return;
    requested.current = true;

    purchaseProduct.mutate(
      { data: { productId, paymentKey, orderId, amount } },
      {
        onSuccess: (transaction) => {
          router.navigate({
            to: "/buyer/checkout-complete",
            search: { transactionId: transaction.transactionId },
          });
        },
        onError: () => setFailed(true),
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (failed) {
    return (
      <PageContainer>
        <Header title="결제 확인" onBack={() => router.navigate({ to: "/" })} />
        <div className="flex flex-col items-center gap-4 px-4 pt-16 text-center">
          <p className="text-sm text-[var(--color-text-muted)]">
            결제는 완료됐지만 거래 처리에 실패했어요.
            <br />
            잠시 후 다시 시도해 주세요.
          </p>
          <Button
            onClick={() =>
              router.navigate({
                to: "/buyer/products/$id",
                params: { id: String(productId) },
              })
            }
          >
            상품으로 돌아가기
          </Button>
        </div>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <Header title="결제 확인" onBack={() => router.navigate({ to: "/" })} />
      <p className="px-4 pt-16 text-center text-sm text-[var(--color-text-muted)]">
        결제를 확인하는 중이에요...
      </p>
    </PageContainer>
  );
}
