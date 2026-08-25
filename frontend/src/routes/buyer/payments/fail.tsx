import { createFileRoute, useRouter } from "@tanstack/react-router";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { Button } from "@/components/ui/button";

type PaymentFailSearch = {
  productId?: number;
  code?: string;
  message?: string;
};

export const Route = createFileRoute("/buyer/payments/fail")({
  validateSearch: (search: Record<string, unknown>): PaymentFailSearch => ({
    productId: search.productId != null ? Number(search.productId) : undefined,
    code: search.code != null ? String(search.code) : undefined,
    message: search.message != null ? String(search.message) : undefined,
  }),
  component: PaymentFailPage,
});

/** 토스 결제창이 실패/취소 시 리다이렉트하는 화면. */
function PaymentFailPage() {
  const router = useRouter();
  const { productId, message } = Route.useSearch();

  const goBack = () => {
    if (productId != null) {
      router.navigate({
        to: "/buyer/products/$id",
        params: { id: String(productId) },
      });
    } else {
      router.navigate({ to: "/" });
    }
  };

  return (
    <PageContainer>
      <Header title="결제 실패" onBack={goBack} />
      <div className="flex flex-col items-center gap-4 px-4 pt-16 text-center">
        <p className="text-sm text-[var(--color-text-muted)]">
          {message ?? "결제가 완료되지 않았어요."}
        </p>
        <Button onClick={goBack}>다시 시도하기</Button>
      </div>
    </PageContainer>
  );
}
