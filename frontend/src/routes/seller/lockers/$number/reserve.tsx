import { useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { ItemRow } from "@/components/domain/item-row";
import { RegisterProductChip } from "@/components/domain/register-product-chip";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { formatPrice } from "@/lib/format";
import {
  useGetMyProducts,
  useReserveLocker,
} from "@/api/generated/products/products";
import { useChangeLockStatus } from "@/api/generated/lockers/lockers";

export const Route = createFileRoute("/seller/lockers/$number/reserve")({
  component: ReservePage,
});

/**
 * 사물함 예약 · 상품 선택 (Figma "05 사물함 예약 · 상품 선택").
 * 예약 확정 시 "08 모달 · 사물함 잠금 해제" 를 띄우고, 확인하면 홈으로 돌아간다.
 */
function ReservePage() {
  const { number } = Route.useParams();
  const lockerId = Number(number);
  const router = useRouter();
  const queryClient = useQueryClient();

  // 사물함 한 칸에는 상품을 하나만 넣을 수 있어 단일 선택으로 동작한다.
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [unlocked, setUnlocked] = useState(false);

  const { data: myProductsData, isLoading } = useGetMyProducts({
    status: "PREPARING",
  });
  const products = myProductsData?.products ?? [];

  const reserveLocker = useReserveLocker();
  const changeLockStatus = useChangeLockStatus();

  const isSubmitting = reserveLocker.isPending || changeLockStatus.isPending;

  const handleConfirm = () => {
    if (!selectedId) return;
    reserveLocker.mutate(
      { productId: selectedId, data: { lockerId } },
      {
        onSuccess: () => {
          changeLockStatus.mutate(
            { lockerId, data: { lockStatus: "UNLOCKED" } },
            {
              onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: ["/lockers"] });
                queryClient.invalidateQueries({ queryKey: ["/products/me"] });
                setUnlocked(true);
              },
              onError: () => toast.error("사물함을 여는 데 실패했어요."),
            },
          );
        },
        onError: () => toast.error("사물함 예약에 실패했어요."),
      },
    );
  };

  return (
    <PageContainer>
      <Header
        title={`${number}번 사물함 예약`}
        onBack={() => router.history.back()}
      />

      <div className="flex flex-col gap-2.5 px-4 pt-2">
        <div className="flex items-center justify-between">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            판매할 상품 선택
          </h2>
          <span className="text-xs text-[var(--color-text-muted)]">
            {selectedId ? 1 : 0}개 선택됨
          </span>
        </div>

        <RegisterProductChip />

        {isLoading ? (
          <p className="py-6 text-center text-sm text-[var(--color-text-muted)]">
            불러오는 중...
          </p>
        ) : products.length === 0 ? (
          <p className="py-6 text-center text-sm text-[var(--color-text-muted)]">
            아직 배치되지 않은 등록 상품이 없어요.
          </p>
        ) : (
          <div className="flex flex-col gap-2.5">
            {products.map((product) => (
              <ItemRow
                key={product.productId}
                title={product.name}
                place={formatPrice(product.price)}
                address="판매 준비중"
                thumbnailUrl={product.imageUrl ?? undefined}
                selectable
                checked={selectedId === product.productId}
                onCheckedChange={(checked) =>
                  setSelectedId(checked ? product.productId : null)
                }
              />
            ))}
          </div>
        )}

        <p className="text-xs text-[var(--color-text-muted)]">
          선택한 상품은 예약 확정 후 {number}번 사물함에 등록돼요.
        </p>
      </div>

      <div className="mt-auto flex flex-col border-t border-[var(--color-border)] px-4 py-3">
        <Button
          fullWidth
          size="lg"
          disabled={!selectedId || isSubmitting}
          onClick={handleConfirm}
        >
          예약 확정하기
        </Button>
      </div>

      <Dialog
        open={unlocked}
        onOpenChange={(open) => {
          if (!open) router.navigate({ to: "/" });
        }}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {number}번 사물함이 열렸어요
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li>· 상품을 넣은 뒤 문을 닫으면 자동으로 잠겨요.</li>
            <li>
              · 점유 기간은 최대 7일이에요.
              <br />
              <span className="font-bold text-[var(--color-text-sub)]">
                오늘부터 7일 후 자동 만료
              </span>
            </li>
            <li>
              · 기간이 끝나기 전까지 판매되지 않으면 상품을 회수해 주세요.
            </li>
          </ul>

          <div className="flex gap-1.5 rounded-[var(--radius-sm)] bg-[var(--color-primary-weak)] p-3 text-[var(--color-primary)]">
            <span className="text-[13px] font-bold">!</span>
            <p className="text-xs font-medium">
              문이 완전히 닫혔는지 반드시 확인해 주세요.
              <br />
              열린 채로 두면 분실 책임이 판매자에게 있어요.
            </p>
          </div>

          <Button
            fullWidth
            size="lg"
            onClick={() => router.navigate({ to: "/" })}
          >
            확인
          </Button>
        </DialogContent>
      </Dialog>
    </PageContainer>
  );
}
