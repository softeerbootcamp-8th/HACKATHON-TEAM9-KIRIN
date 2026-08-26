import { useEffect, useRef, useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import {
  ANONYMOUS,
  loadTossPayments,
  type TossPaymentsWidgets,
} from "@tosspayments/tosspayments-sdk";
import { toast } from "sonner";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { ItemRow } from "@/components/domain/item-row";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";
import { useGetProduct } from "@/api/generated/products/products";
import { usePurchaseProductForDemo } from "@/api/generated/transactions/transactions";

type CheckoutSearch = { productId: number };

export const Route = createFileRoute("/buyer/checkout")({
  validateSearch: (search: Record<string, unknown>): CheckoutSearch => ({
    productId: Number(search.productId),
  }),
  component: CheckoutPage,
});

const PAYMENT_METHOD_SELECTOR = "#toss-payment-method";
const AGREEMENT_SELECTOR = "#toss-agreement";

/**
 * 결제 (Figma "10 결제"). 결제수단은 커스텀 버튼 대신 토스페이먼츠
 * 결제위젯(`widgets()` API)이 렌더링하는 UI를 그대로 쓴다 — `payment()` API의
 * `method` 파라미터로는 카카오페이를 직접 지정할 방법이 문서화되어 있지 않다.
 */
function CheckoutPage() {
  const router = useRouter();
  const { productId } = Route.useSearch();
  const { data: product, isLoading } = useGetProduct(productId);

  const [widgets, setWidgets] = useState<TossPaymentsWidgets | null>(null);
  const [isPaying, setIsPaying] = useState(false);
  const orderIdRef = useRef(`order-${crypto.randomUUID()}`);
  const purchaseProductForDemo = usePurchaseProductForDemo();

  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;

  useEffect(() => {
    if (!product || !clientKey || product.status !== "SELLING") return;
    let active = true;

    (async () => {
      const tossPayments = await loadTossPayments(clientKey);
      const instance = tossPayments.widgets({ customerKey: ANONYMOUS });
      await instance.setAmount({ currency: "KRW", value: product.price });
      await Promise.all([
        instance.renderPaymentMethods({ selector: PAYMENT_METHOD_SELECTOR }),
        instance.renderAgreement({ selector: AGREEMENT_SELECTOR }),
      ]);
      if (active) setWidgets(instance);
    })();

    return () => {
      active = false;
      document.querySelector(PAYMENT_METHOD_SELECTOR)?.replaceChildren();
      document.querySelector(AGREEMENT_SELECTOR)?.replaceChildren();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [product?.productId, clientKey]);

  const handlePay = async () => {
    if (!widgets || !product || isPaying) return;
    setIsPaying(true);
    try {
      // 결제 성공/실패 화면에서 "상품으로 돌아가기"가 진열함 번호 기반
      // 상품 상세(`/buyer/lockers/$number`)로 이동해야 해서 lockerId도 같이 넘긴다.
      const returnParams = new URLSearchParams({
        productId: String(product.productId),
        ...(product.lockerId != null
          ? { lockerId: String(product.lockerId) }
          : {}),
      });
      await widgets.requestPayment({
        orderId: orderIdRef.current,
        orderName: product.name,
        successUrl: `${window.location.origin}/buyer/payments/success?${returnParams}`,
        failUrl: `${window.location.origin}/buyer/payments/fail?${returnParams}`,
      });
    } catch (error) {
      if (error instanceof Error && error.name === "UserCancelError") return;
      toast.error("결제창을 여는 데 실패했어요.");
    } finally {
      setIsPaying(false);
    }
  };

  /**
   * 데모용 간편결제: 토스 결제위젯을 열지 않고 버튼 한 번으로 바로 결제·거래
   * 생성까지 끝낸다(`POST /transactions/demo-purchase`). 실제 결제창을 거치지
   * 않으므로 successUrl 리다이렉트 없이 완료 화면으로 바로 이동한다.
   */
  const handleDemoPay = () => {
    if (!product || isPaying || purchaseProductForDemo.isPending) return;
    purchaseProductForDemo.mutate(
      { data: { productId: product.productId } },
      {
        onSuccess: (transaction) => {
          router.navigate({
            to: "/buyer/checkout-complete",
            search: { transactionId: transaction.transactionId },
          });
        },
        onError: () => {
          toast.error("간편결제에 실패했어요.");
        },
      },
    );
  };

  if (isLoading || !product) {
    return (
      <PageContainer>
        <Header title="결제" onBack={() => router.history.back()} />
        <p className="px-4 pt-10 text-center text-sm text-[var(--color-text-muted)]">
          불러오는 중...
        </p>
      </PageContainer>
    );
  }

  if (product.status !== "SELLING") {
    return (
      <PageContainer>
        <Header title="결제" onBack={() => router.history.back()} />
        <p className="px-4 pt-10 text-center text-sm text-[var(--color-text-muted)]">
          지금은 구매할 수 없는 상품이에요.
        </p>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <Header title="결제" onBack={() => router.history.back()} />

      <div className="flex flex-col gap-4 px-4 pt-2">
        <section className="flex flex-col gap-2">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            주문 상품
          </h2>
          <ItemRow
            title={product.name}
            place={formatPrice(product.price)}
            address={
              product.lockerId != null
                ? `${product.lockerId}번 진열함`
                : "진열함 미배치"
            }
            thumbnailUrl={product.imageUrls[0]}
          />
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            간편 결제
          </h2>
          <Button
            fullWidth
            size="lg"
            variant="outline"
            disabled={isPaying || purchaseProductForDemo.isPending}
            onClick={handleDemoPay}
          >
            {purchaseProductForDemo.isPending
              ? "결제 처리 중..."
              : `${formatPrice(product.price)} 간편결제로 바로 구매`}
          </Button>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            결제 수단
          </h2>
          <div id="toss-payment-method" />
          <div id="toss-agreement" />
          {!widgets && (
            <p className="py-6 text-center text-sm text-[var(--color-text-muted)]">
              결제 수단을 불러오는 중...
            </p>
          )}
        </section>

        <p className="text-xs text-[var(--color-text-muted)]">
          결제 후에는 진열함이 즉시 열리며, 단순 변심에 의한 취소가 불가해요.
          주문 내용을 확인했으며 결제에 동의합니다.
        </p>
      </div>

      <div className="mt-auto flex flex-col border-t border-[var(--color-border)] px-4 py-3">
        <Button
          fullWidth
          size="lg"
          disabled={!widgets || isPaying || purchaseProductForDemo.isPending}
          onClick={handlePay}
        >
          {formatPrice(product.price)} 결제하기
        </Button>
      </div>
    </PageContainer>
  );
}
