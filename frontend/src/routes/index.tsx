import { useMemo, useState } from "react";
import { createFileRoute, useRouter, Link } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { PageContainer } from "@/components/layout/page";
import { Logo } from "@/components/layout/logo";
import {
  LockerGrid,
  type LockerGridItem,
} from "@/components/domain/locker-grid";
import { ItemRow } from "@/components/domain/item-row";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  BottomSheet,
  BottomSheetContent,
  BottomSheetHeader,
  BottomSheetTitle,
  BottomSheetBody,
} from "@/components/ui/bottom-sheet";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { formatDateTime, formatPrice, formatRemaining } from "@/lib/format";
import {
  useGetLockers,
  getGetLockersQueryKey,
  useChangeLockStatus,
} from "@/api/generated/lockers/lockers";
import {
  useGetMyProducts,
  useGetProduct,
  getGetMyProductsQueryKey,
  useCancelLockerReservation,
  useStartDeposit,
  useStartRecovery,
} from "@/api/generated/products/products";
import type { ProductResponse } from "@/api/generated/model";

export const Route = createFileRoute("/")({
  component: HomePage,
});

type HomeLocker = LockerGridItem & {
  /** 본인이 예약/판매 중인 사물함인지 — 아니면 08-1 안내 모달 또는 토스트만 노출 */
  isMine: boolean;
  productId?: number;
};

// 사물함이 전부 동일 규격이라 기본값으로 둔다 — 백엔드에 규격 필드가 아직 없다.
const DEFAULT_LOCKER_SIZE = "40 × 100 × 50 cm";
const DEFAULT_MAX_OCCUPANCY_DAYS = 7;

type SheetState =
  | { type: "empty"; locker: HomeLocker }
  | { type: "reserved"; locker: HomeLocker }
  | { type: "selling"; locker: HomeLocker };

/** Sheet/InfoBox — 예약중/판매중 바텀시트에서 쓰는 라벨·값 정보 박스 */
function InfoBox({
  rows,
}: {
  rows: { label: string; value: string; tone?: "danger" }[];
}) {
  return (
    <div className="flex w-full flex-col gap-2.5 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-2)] p-3.5 text-[13px]">
      {rows.map((row) => (
        <div key={row.label} className="flex items-center justify-between">
          <span className="text-[var(--color-text-muted)]">{row.label}</span>
          <span
            className={
              row.tone === "danger"
                ? "font-medium text-[var(--color-danger)]"
                : "font-medium text-[var(--color-text-sub)]"
            }
          >
            {row.value}
          </span>
        </div>
      ))}
    </div>
  );
}

/**
 * 홈 · 사물함 현황 (Figma "01 홈 · 사물함 현황" + "02/03/04 바텀시트" +
 * "08-1 예약중입니다"). 로그인 게이트는 두지 않는다 — SessionProvider가
 * 부팅 시 게스트 세션을 조용히 만들어 둔다.
 *
 * `GET /lockers`(상태)와 `GET /products/me`(내 상품 요약, 소유권 판별용)를
 * lockerId로 합성해서 그리드를 만든다. 바텀시트를 열 때만 해당 상품의
 * 상세(`GET /products/{id}`)를 조회해 날짜 등 상세 정보를 채운다.
 */
function HomePage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [sheet, setSheet] = useState<SheetState | null>(null);
  const [infoModalLocker, setInfoModalLocker] = useState<HomeLocker | null>(
    null,
  );

  const { data: lockersData, isLoading: isLockersLoading } = useGetLockers();
  const { data: myProductsData } = useGetMyProducts({});

  const lockers: HomeLocker[] = useMemo(() => {
    const myProductByLockerId = new Map(
      (myProductsData?.products ?? [])
        .filter((product) => product.lockerId != null)
        .map((product) => [product.lockerId as number, product]),
    );

    return (lockersData?.lockers ?? []).map((locker) => {
      const myProduct = myProductByLockerId.get(locker.lockerId);
      const status: LockerGridItem["status"] =
        locker.usageStatus === "AVAILABLE"
          ? "empty"
          : locker.usageStatus === "RESERVED"
            ? "reserved"
            : "selling";

      return {
        number: locker.lockerId,
        status,
        isMine: myProduct != null,
        productId: myProduct?.productId,
      };
    });
  }, [lockersData, myProductsData]);

  const selectedProductId =
    sheet?.type === "reserved" || sheet?.type === "selling"
      ? sheet.locker.productId
      : undefined;
  const { data: selectedProduct } = useGetProduct(selectedProductId ?? 0, {
    query: { enabled: selectedProductId != null },
  });

  const invalidateLockerData = () => {
    queryClient.invalidateQueries({ queryKey: getGetLockersQueryKey() });
    queryClient.invalidateQueries({ queryKey: getGetMyProductsQueryKey({}) });
  };

  const changeLockStatus = useChangeLockStatus();
  const cancelReservation = useCancelLockerReservation();
  const startDeposit = useStartDeposit();
  const startRecovery = useStartRecovery();

  const closeSheet = () => setSheet(null);

  const handleSelect = (number: number) => {
    const locker = lockers.find((item) => item.number === number);
    if (!locker) return;

    if (locker.status === "empty") {
      setSheet({ type: "empty", locker });
      return;
    }
    if (locker.status === "reserved") {
      if (locker.isMine) setSheet({ type: "reserved", locker });
      else setInfoModalLocker(locker);
      return;
    }
    // selling
    if (locker.isMine) setSheet({ type: "selling", locker });
    else toast.info("판매 중인 사물함입니다.");
  };

  const handleCancelReservation = (locker: HomeLocker) => {
    if (!locker.productId) return;
    cancelReservation.mutate(
      { productId: locker.productId },
      {
        onSuccess: () => {
          toast.success("예약이 취소됐어요.");
          invalidateLockerData();
          closeSheet();
        },
        onError: () => toast.error("예약 취소에 실패했어요."),
      },
    );
  };

  const handleOpenForDeposit = (locker: HomeLocker) => {
    if (!locker.productId) return;
    changeLockStatus.mutate(
      { lockerId: locker.number, data: { lockStatus: "UNLOCKED" } },
      {
        onSuccess: () => {
          startDeposit.mutate(
            { productId: locker.productId! },
            {
              onSuccess: () => {
                toast.success("사물함이 열렸어요.");
                invalidateLockerData();
                closeSheet();
              },
              onError: () => toast.error("입고 처리에 실패했어요."),
            },
          );
        },
        onError: () => toast.error("사물함을 여는 데 실패했어요."),
      },
    );
  };

  const handleEndSelling = (locker: HomeLocker) => {
    if (!locker.productId) return;
    changeLockStatus.mutate(
      { lockerId: locker.number, data: { lockStatus: "UNLOCKED" } },
      {
        onSuccess: () => {
          startRecovery.mutate(
            { productId: locker.productId! },
            {
              onSuccess: () => {
                toast.success("판매가 종료됐어요.");
                invalidateLockerData();
                closeSheet();
              },
              onError: () => toast.error("판매 종료 처리에 실패했어요."),
            },
          );
        },
        onError: () => toast.error("사물함을 여는 데 실패했어요."),
      },
    );
  };

  return (
    <PageContainer>
      <Logo />

      <div className="flex items-center justify-between px-4 pt-2">
        <h1 className="text-xl font-bold text-[var(--color-text)]">
          사물함 현황
        </h1>
        <Link
          to="/seller/my-list"
          className="flex items-center gap-0.5 rounded-[var(--radius-pill)] border border-[var(--color-border)] bg-[var(--color-surface-2)] px-2.5 py-1.5 text-[13px]"
        >
          <span className="text-[var(--color-text-sub)]">내 리스트</span>
          <span className="text-[var(--color-text-muted)]">›</span>
        </Link>
      </div>

      <ul className="flex items-center gap-3.5 px-4 pt-4 text-xs text-[var(--color-text-muted)]">
        <li className="flex items-center gap-1">
          <span className="size-2 rounded-full bg-[var(--color-border)]" />
          비어있음
        </li>
        <li className="flex items-center gap-1">
          <span className="size-2 rounded-full bg-[var(--color-info)]" />
          예약중
        </li>
        <li className="flex items-center gap-1">
          <span className="size-2 rounded-full bg-[var(--color-danger)]" />
          판매중
        </li>
      </ul>

      {isLockersLoading ? (
        <p className="px-4 pt-6 text-center text-sm text-[var(--color-text-muted)]">
          불러오는 중...
        </p>
      ) : (
        <LockerGrid
          lockers={lockers}
          onSelect={handleSelect}
          className="px-4 pt-4"
        />
      )}

      <BottomSheet
        open={sheet !== null}
        onOpenChange={(open) => !open && closeSheet()}
      >
        <BottomSheetContent>
          {sheet?.type === "empty" && (
            <>
              <BottomSheetHeader>
                <BottomSheetTitle>
                  {sheet.locker.number}번 사물함
                </BottomSheetTitle>
              </BottomSheetHeader>
              <BottomSheetBody className="flex flex-col items-center gap-10 pt-10 pb-4">
                <p className="text-center text-sm text-[var(--color-text-muted)]">
                  비어있는 사물함이에요
                  <br />
                  {DEFAULT_LOCKER_SIZE} · 최대 {DEFAULT_MAX_OCCUPANCY_DAYS}일
                  점유 가능
                </p>
                <Button
                  fullWidth
                  size="lg"
                  onClick={() => {
                    const number = sheet.locker.number;
                    closeSheet();
                    router.navigate({
                      to: "/seller/lockers/$number/reserve",
                      params: { number: String(number) },
                    });
                  }}
                >
                  예약하기
                </Button>
              </BottomSheetBody>
            </>
          )}

          {sheet?.type === "reserved" && selectedProduct && (
            <ReservedSheetBody
              locker={sheet.locker}
              product={selectedProduct}
              onCancel={() => handleCancelReservation(sheet.locker)}
              onOpen={() => handleOpenForDeposit(sheet.locker)}
              pending={
                cancelReservation.isPending || changeLockStatus.isPending
              }
            />
          )}

          {sheet?.type === "selling" && selectedProduct && (
            <SellingSheetBody
              locker={sheet.locker}
              product={selectedProduct}
              onEndSelling={() => handleEndSelling(sheet.locker)}
              pending={changeLockStatus.isPending || startRecovery.isPending}
            />
          )}
        </BottomSheetContent>
      </BottomSheet>

      <Dialog
        open={infoModalLocker !== null}
        onOpenChange={(open) => !open && setInfoModalLocker(null)}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            예약중인 사물함이에요
          </DialogTitle>
          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li>· 다른 사용자가 예약한 사물함이에요.</li>
            <li>· 판매 등록이 완료되면 상품 정보를 확인할 수 있어요.</li>
          </ul>
          <Button fullWidth size="lg" onClick={() => setInfoModalLocker(null)}>
            확인
          </Button>
        </DialogContent>
      </Dialog>
    </PageContainer>
  );
}

function ReservedSheetBody({
  locker,
  product,
  onCancel,
  onOpen,
  pending,
}: {
  locker: HomeLocker;
  product: ProductResponse;
  onCancel: () => void;
  onOpen: () => void;
  pending: boolean;
}) {
  return (
    <>
      <BottomSheetHeader className="h-auto items-center justify-start gap-2">
        <BottomSheetTitle>{locker.number}번 사물함</BottomSheetTitle>
        <Badge variant="info">예약중</Badge>
      </BottomSheetHeader>
      <BottomSheetBody className="flex flex-col gap-3">
        <InfoBox
          rows={[
            { label: "예약 상품", value: product.name },
            {
              label: "점유 기간",
              value:
                product.reservedAt && product.reservationExpiresAt
                  ? `${formatDateTime(product.reservedAt)} ~ ${formatDateTime(product.reservationExpiresAt)}`
                  : "-",
            },
            {
              label: "남은 시간",
              value: product.reservationExpiresAt
                ? formatRemaining(product.reservationExpiresAt)
                : "-",
            },
          ]}
        />
        <p className="text-xs text-[var(--color-text-muted)]">
          예약 후 최대 4시간 안에 상품을 넣지 않으면 예약이 자동 취소돼요.
        </p>
        <div className="flex gap-2 pt-2">
          <Button
            variant="secondary"
            size="lg"
            className="flex-1"
            disabled={pending}
            onClick={onCancel}
          >
            예약 취소
          </Button>
          <Button
            size="lg"
            className="flex-1"
            disabled={pending}
            onClick={onOpen}
          >
            사물함 열기
          </Button>
        </div>
      </BottomSheetBody>
    </>
  );
}

function SellingSheetBody({
  locker,
  product,
  onEndSelling,
  pending,
}: {
  locker: HomeLocker;
  product: ProductResponse;
  onEndSelling: () => void;
  pending: boolean;
}) {
  return (
    <>
      <BottomSheetHeader className="h-auto items-center justify-start gap-2">
        <BottomSheetTitle>{locker.number}번 사물함</BottomSheetTitle>
        <Badge variant="danger">판매중</Badge>
      </BottomSheetHeader>
      <BottomSheetBody className="flex flex-col gap-3">
        <ItemRow
          title={product.name}
          place={formatPrice(product.price)}
          address={product.imageUrl ? "" : "사진 없음"}
          thumbnailUrl={product.imageUrl ?? undefined}
        />
        <InfoBox
          rows={[
            {
              label: "판매 시작",
              value: product.sellingStartedAt
                ? formatDateTime(product.sellingStartedAt)
                : "-",
            },
            {
              label: "점유 만료",
              value: product.sellingExpiresAt
                ? formatDateTime(product.sellingExpiresAt)
                : "-",
            },
            {
              label: "남은 점유 기간",
              value: product.sellingExpiresAt
                ? formatRemaining(product.sellingExpiresAt)
                : "-",
              tone: "danger",
            },
          ]}
        />
        <p className="text-xs text-[var(--color-text-muted)]">
          판매를 종료하면 사물함이 열려요. 상품을 회수한 뒤 문을 닫아 주세요.
        </p>
        <Button fullWidth size="lg" disabled={pending} onClick={onEndSelling}>
          판매 종료
        </Button>
      </BottomSheetBody>
    </>
  );
}
