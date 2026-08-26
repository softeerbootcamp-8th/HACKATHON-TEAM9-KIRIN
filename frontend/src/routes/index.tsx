import { useEffect, useMemo, useState } from "react";
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
import {
  formatCountdown,
  formatDateTime,
  formatDday,
  formatOccupancyPeriod,
  formatPrice,
  formatRemaining,
  formatRemainingDetailed,
} from "@/lib/format";
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

/**
 * "05 사물함 예약 · 상품 선택" 에서 "자리 예약"을 누르면 사물함 번호를 쿼리
 * 파라미터로 들고 홈으로 돌아온다 — 홈은 사물함 목록을 불러온 뒤 해당 사물함의
 * 예약중 바텀시트를 바로 띄운다.
 */
type HomeSearch = {
  openLocker?: number;
};

export const Route = createFileRoute("/")({
  validateSearch: (search: Record<string, unknown>): HomeSearch => ({
    openLocker:
      search.openLocker != null ? Number(search.openLocker) : undefined,
  }),
  component: HomePage,
});

type HomeLocker = LockerGridItem & {
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
  rows: { label: string; value: string; tone?: "danger" | "mine" }[];
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
                : row.tone === "mine"
                  ? "font-medium text-[var(--color-mine)]"
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
 * `GET /lockers`가 소유 여부(isMine)와 남은 예약 시간/판매 일수를 이미 담아
 * 주므로 그리드 표시는 그 응답만으로 만든다. 본인 소유 사물함을 눌러 바텀
 * 시트를 열 때만 `GET /products/me`로 productId를 찾고, 이어서
 * `GET /products/{id}`로 이름·가격 등 상세 정보를 채운다.
 */
function HomePage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const search = Route.useSearch();
  const [sheet, setSheet] = useState<SheetState | null>(null);
  const [infoModalLocker, setInfoModalLocker] = useState<HomeLocker | null>(
    null,
  );
  // "진열함 열기" 확인 모달 — 개방 요청이 성공하면 뜬다.
  const [openedLocker, setOpenedLocker] = useState<HomeLocker | null>(null);
  // 점유 기한 안내("8/25(월) 15:10 - 8/31(일) 15:10")를 계산하는 기준 시각 — 투입 시작 응답을 그대로 쓴다.
  const [openedLockerDepositedAt, setOpenedLockerDepositedAt] =
    useState<Date | null>(null);

  const { data: lockersData, isLoading: isLockersLoading } = useGetLockers();
  const { data: myProductsData } = useGetMyProducts({});

  // 예약중 셀의 남은 시간을 초 단위로 실시간으로 보여주기 위한 틱.
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const lockers: HomeLocker[] = useMemo(() => {
    const myProductByLockerId = new Map(
      (myProductsData?.products ?? [])
        .filter((product) => product.lockerId != null)
        .map((product) => [product.lockerId as number, product]),
    );

    return (lockersData?.lockers ?? []).map((locker) => {
      // 타인 소유(비로그인 포함)면 백엔드가 RESERVED/OCCUPIED를 구분 없이
      // 전부 OCCUPIED로 내려주므로, 표시 상태는 usageStatus + isMine만으로 정해진다.
      let status: LockerGridItem["status"];
      let detail: string | undefined;
      if (locker.usageStatus === "AVAILABLE") {
        status = "empty";
      } else if (locker.usageStatus === "RESERVED") {
        status = "reserved";
        detail = locker.reservationExpiresAt
          ? formatCountdown(locker.reservationExpiresAt, now)
          : undefined;
      } else if (locker.isMine) {
        status = "selling";
        detail = locker.sellingExpiresAt
          ? formatDday(locker.sellingExpiresAt, now)
          : undefined;
      } else {
        status = "occupied";
      }

      return {
        number: locker.lockerId,
        status,
        detail,
        productId: myProductByLockerId.get(locker.lockerId)?.productId,
      };
    });
  }, [lockersData, myProductsData, now]);

  // "자리 예약" 직후 돌아온 경우 — 쿼리 파라미터로 받은 사물함이 사물함 목록에
  // 반영되면(예약중·내 것) 그 바텀시트를 자동으로 띄운다. sheet state 를 별도
  // effect 로 동기화하지 않고 렌더링 중에 파생시킨다.
  const autoOpenLocker =
    search.openLocker != null
      ? lockers.find(
          (item) =>
            item.number === search.openLocker && item.status === "reserved",
        )
      : undefined;
  const effectiveSheet: SheetState | null =
    sheet ??
    (autoOpenLocker ? { type: "reserved", locker: autoOpenLocker } : null);

  const selectedProductId =
    effectiveSheet?.type === "reserved" || effectiveSheet?.type === "selling"
      ? effectiveSheet.locker.productId
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

  const closeSheet = () => {
    setSheet(null);
    if (search.openLocker != null) {
      router.navigate({ to: "/", search: {}, replace: true });
    }
  };

  const handleSelect = (number: number) => {
    const locker = lockers.find((item) => item.number === number);
    if (!locker) return;

    switch (locker.status) {
      case "empty":
        setSheet({ type: "empty", locker });
        return;
      case "reserved":
        setSheet({ type: "reserved", locker });
        return;
      case "selling":
        setSheet({ type: "selling", locker });
        return;
      case "occupied":
        // 타인이 예약/판매 중인 사물함 — 어느 쪽인지는 구분하지 않고 안내만 띄운다.
        setInfoModalLocker(locker);
    }
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
              onSuccess: (product) => {
                invalidateLockerData();
                closeSheet();
                setOpenedLocker(locker);
                setOpenedLockerDepositedAt(
                  product.depositStartedAt
                    ? new Date(product.depositStartedAt)
                    : new Date(),
                );
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
          사용중
        </li>
        <li className="flex items-center gap-1">
          <span className="size-2 rounded-full bg-[var(--color-mine)]" />내 상품
        </li>
        <li className="flex items-center gap-1">
          <span className="size-2 rounded-full bg-[var(--color-primary)]" />
          비어있음
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
        open={effectiveSheet !== null}
        onOpenChange={(open) => !open && closeSheet()}
      >
        <BottomSheetContent>
          {effectiveSheet?.type === "empty" && (
            <>
              <BottomSheetHeader>
                <BottomSheetTitle>
                  {effectiveSheet.locker.number}번 사물함
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
                    const number = effectiveSheet.locker.number;
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

          {effectiveSheet?.type === "reserved" && selectedProduct && (
            <ReservedSheetBody
              locker={effectiveSheet.locker}
              product={selectedProduct}
              now={now}
              onCancel={() => handleCancelReservation(effectiveSheet.locker)}
              onOpen={() => handleOpenForDeposit(effectiveSheet.locker)}
              pending={
                cancelReservation.isPending || changeLockStatus.isPending
              }
            />
          )}

          {effectiveSheet?.type === "selling" && selectedProduct && (
            <SellingSheetBody
              locker={effectiveSheet.locker}
              product={selectedProduct}
              onEndSelling={() => handleEndSelling(effectiveSheet.locker)}
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
            사용 중인 사물함이에요
          </DialogTitle>
          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li>· 다른 사용자가 예약 또는 판매 중인 사물함이에요.</li>
            <li>· 이용이 끝나면 다시 예약할 수 있어요.</li>
          </ul>
          <Button fullWidth size="lg" onClick={() => setInfoModalLocker(null)}>
            확인
          </Button>
        </DialogContent>
      </Dialog>

      <Dialog
        open={openedLocker !== null}
        onOpenChange={(open) => {
          if (!open) {
            setOpenedLocker(null);
            setOpenedLockerDepositedAt(null);
          }
        }}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {openedLocker?.number}번 진열함이 열렸어요
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li>· 상품을 넣은 뒤 문을 닫으면 자동으로 잠겨요.</li>
            <li>
              · 점유 기간은 최대 {DEFAULT_MAX_OCCUPANCY_DAYS}일이에요.
              {openedLockerDepositedAt && (
                <>
                  <br />
                  <span className="font-bold text-[var(--color-text-sub)]">
                    {formatOccupancyPeriod(
                      openedLockerDepositedAt,
                      DEFAULT_MAX_OCCUPANCY_DAYS,
                    )}
                  </span>
                </>
              )}
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
            onClick={() => {
              setOpenedLocker(null);
              setOpenedLockerDepositedAt(null);
            }}
          >
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
  now,
  onCancel,
  onOpen,
  pending,
}: {
  locker: HomeLocker;
  product: ProductResponse;
  now: Date;
  onCancel: () => void;
  onOpen: () => void;
  pending: boolean;
}) {
  return (
    <>
      <BottomSheetHeader className="h-auto items-center justify-start gap-2">
        <BottomSheetTitle>{locker.number}번 진열함</BottomSheetTitle>
        <Badge variant="mine">예약중</Badge>
        <Link
          to="/seller/products/$productId"
          params={{ productId: String(product.productId) }}
          className="ml-auto text-xs text-[var(--color-text-muted)]"
        >
          상세보기 ›
        </Link>
      </BottomSheetHeader>
      <BottomSheetBody className="flex flex-col gap-3">
        <InfoBox
          rows={[
            { label: "예약 상품", value: product.name },
            {
              label: "남은 예약 시간",
              value: product.reservationExpiresAt
                ? formatRemainingDetailed(product.reservationExpiresAt, now)
                : "-",
              tone: "mine",
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
            진열함 열기
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
  const router = useRouter();

  return (
    <>
      <BottomSheetHeader className="h-auto items-center justify-start gap-2">
        <BottomSheetTitle>{locker.number}번 진열함</BottomSheetTitle>
        <Badge variant="mineSelling">판매중</Badge>
      </BottomSheetHeader>
      <BottomSheetBody className="flex flex-col gap-3">
        <ItemRow
          title={product.name}
          place={formatPrice(product.price)}
          address={product.imageUrls[0] ? "" : "사진 없음"}
          thumbnailUrl={product.imageUrls[0]}
          onClick={() =>
            router.navigate({
              to: "/seller/products/$productId",
              params: { productId: String(product.productId) },
            })
          }
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
              tone: "mine",
            },
          ]}
        />
        <p className="text-xs text-[var(--color-text-muted)]">
          판매를 종료하면 진열함이 열려요. 상품을 회수한 뒤 문을 닫아 주세요.
        </p>
        <Button fullWidth size="lg" disabled={pending} onClick={onEndSelling}>
          판매 종료
        </Button>
      </BottomSheetBody>
    </>
  );
}
