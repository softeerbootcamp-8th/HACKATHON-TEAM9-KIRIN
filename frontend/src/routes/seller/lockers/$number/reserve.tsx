import { useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "sonner";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { ItemRow } from "@/components/domain/item-row";
import { RegisterProductChip } from "@/components/domain/register-product-chip";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { formatOccupancyPeriod, formatPrice } from "@/lib/format";
import {
  useGetMyProducts,
  useReserveLocker,
} from "@/api/generated/products/products";
import { useChangeLockStatus } from "@/api/generated/lockers/lockers";
import type { ErrorResponse } from "@/api/generated/model";

/** 예약 시도 사이에 다른 판매자가 먼저 같은 진열함을 예약한 경우 백엔드가 내려주는 코드. */
const LOCKER_NOT_AVAILABLE = "LOCKER_NOT_AVAILABLE";

function isLockerAlreadyReserved(error: unknown) {
  return (
    axios.isAxiosError<ErrorResponse>(error) &&
    error.response?.data.code === LOCKER_NOT_AVAILABLE
  );
}

export const Route = createFileRoute("/seller/lockers/$number/reserve")({
  component: ReservePage,
});

// 진열함이 전부 동일 규격이라 기본값으로 둔다 — src/routes/index.tsx의
// DEFAULT_MAX_OCCUPANCY_DAYS와 동일한 값(백엔드 SELLING_DAYS).
const MAX_OCCUPANCY_DAYS = 7;

/**
 * 진열함 예약 · 상품 선택 (Figma "05 사물함 예약 · 상품 선택").
 * "자리 예약"은 진열함을 열지 않고 홈의 예약중(본인) 바텀시트로 바로 이동하고,
 * "바로 팔기"는 예약과 동시에 진열함을 열어 "08 모달 · 사물함 잠금 해제" 를
 * 띄운 뒤 확인하면 홈으로 돌아간다.
 */
function ReservePage() {
  const { number } = Route.useParams();
  const lockerId = Number(number);
  const router = useRouter();
  const queryClient = useQueryClient();

  // 진열함 한 칸에는 상품을 하나만 넣을 수 있어 단일 선택으로 동작한다.
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [sellDialogOpen, setSellDialogOpen] = useState(false);
  const [conflictDialogOpen, setConflictDialogOpen] = useState(false);
  // 점유 기한 안내("8/25(월) 15:10 - 8/31(일) 15:10")를 계산하는 기준 시각.
  const [depositedAt, setDepositedAt] = useState<Date | null>(null);

  const { data: myProductsData, isLoading } = useGetMyProducts({
    status: "PREPARING",
  });
  const products = myProductsData?.products ?? [];

  const reserveLocker = useReserveLocker();
  const changeLockStatus = useChangeLockStatus();

  const isSubmitting = reserveLocker.isPending || changeLockStatus.isPending;

  // 마운트 여부와 상관없이 즉시 API를 다시 호출해 최신 사물함 현황을 캐시에 채워 둔다.
  // (invalidateQueries는 현재 화면에 마운트된 쿼리만 즉시 재요청하므로, 홈이
  // 마운트돼 있지 않은 이 화면에서는 stale 표시만 되고 실제 호출은 홈 마운트 시로 미뤄진다.)
  const invalidateLockerData = () => {
    queryClient.refetchQueries({ queryKey: ["/lockers"], type: "all" });
    queryClient.refetchQueries({ queryKey: ["/products/me"], type: "all" });
  };

  // "자리 예약" — 진열함을 열지 않고 예약만 확정한 뒤 홈의 예약중 바텀시트로 이동한다.
  const handleReserveSpot = () => {
    if (!selectedId) return;
    reserveLocker.mutate(
      { productId: selectedId, data: { lockerId } },
      {
        onSuccess: () => {
          invalidateLockerData();
          router.navigate({ to: "/", search: { openLocker: lockerId } });
        },
        onError: (error) => {
          if (isLockerAlreadyReserved(error)) {
            invalidateLockerData();
            setConflictDialogOpen(true);
            return;
          }
          toast.error("진열함 예약에 실패했어요.");
        },
      },
    );
  };

  // "바로 팔기" — 예약과 동시에 진열함을 열어 즉시 판매 준비를 시작한다.
  const handleSellNow = () => {
    if (!selectedId) return;
    reserveLocker.mutate(
      { productId: selectedId, data: { lockerId } },
      {
        onSuccess: () => {
          changeLockStatus.mutate(
            { lockerId, data: { lockStatus: "UNLOCKED" } },
            {
              onSuccess: () => {
                invalidateLockerData();
                setDepositedAt(new Date());
                setSellDialogOpen(true);
              },
              onError: () => toast.error("진열함을 여는 데 실패했어요."),
            },
          );
        },
        onError: (error) => {
          if (isLockerAlreadyReserved(error)) {
            invalidateLockerData();
            setConflictDialogOpen(true);
            return;
          }
          toast.error("진열함 예약에 실패했어요.");
        },
      },
    );
  };

  return (
    <PageContainer>
      <Header
        title={`${number}번 진열함 예약`}
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
          선택한 상품은 예약 확정 후 {number}번 진열함에 등록돼요.
        </p>
      </div>

      <div className="mt-auto flex gap-4 border-t border-[var(--color-border)] px-4 py-3">
        <Button
          variant="outline"
          size="lg"
          className="flex-1"
          disabled={!selectedId || isSubmitting}
          onClick={handleReserveSpot}
        >
          자리 예약
        </Button>
        <Button
          size="lg"
          className="flex-1"
          disabled={!selectedId || isSubmitting}
          onClick={handleSellNow}
        >
          바로 팔기
        </Button>
      </div>

      <Dialog
        open={sellDialogOpen}
        onOpenChange={(open) => {
          if (!open) router.navigate({ to: "/" });
        }}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {number}번 진열함이 열렸어요
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <p className="flex-1">
                상품을 넣은 뒤 문을 닫으면 자동으로 잠겨요.
              </p>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <div className="flex-1">
                <p>점유 기간은 최대 {MAX_OCCUPANCY_DAYS}일이에요.</p>
                {depositedAt && (
                  <p className="font-bold">
                    {formatOccupancyPeriod(depositedAt, MAX_OCCUPANCY_DAYS)}
                  </p>
                )}
              </div>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <p className="flex-1">
                기간이 끝나기 전까지 판매되지 않으면 상품을 회수해 주세요.
              </p>
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

      <Dialog
        open={conflictDialogOpen}
        onOpenChange={(open) => {
          if (!open) router.navigate({ to: "/" });
        }}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            이미 예약된 진열함이에요
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li>· 다른 사용자가 {number}번 진열함을 예약했어요.</li>
            <li>· 판매 등록이 끝나면 상품 정보를 확인할 수 있어요.</li>
            <li>· 예약 후 4시간 안에 등록되지 않으면 자동으로 취소돼요.</li>
          </ul>

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
