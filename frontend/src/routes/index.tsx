import { useEffect, useState } from "react";
import { createFileRoute, useRouter, Link } from "@tanstack/react-router";
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

/**
 * "05 사물함 예약 · 상품 선택" 에서 "자리 예약"/"바로 팔기" 를 누르면 해당 사물함
 * 번호와 상품 정보를 쿼리 파라미터로 들고 홈으로 돌아온다 — 홈은 이 값을 읽어
 * 사물함 상태를 갱신하고(예약의 경우) 바텀시트를 바로 띄운다.
 */
type HomeSearch = {
  justReserved?: number;
  justSold?: number;
  product?: string;
  price?: string;
};

export const Route = createFileRoute("/")({
  validateSearch: (search: Record<string, unknown>): HomeSearch => ({
    justReserved:
      search.justReserved != null ? Number(search.justReserved) : undefined,
    justSold: search.justSold != null ? Number(search.justSold) : undefined,
    product: typeof search.product === "string" ? search.product : undefined,
    price: typeof search.price === "string" ? search.price : undefined,
  }),
  component: HomePage,
});

const WEEKDAY_KO = ["일", "월", "화", "수", "목", "금", "토"];
// 예약 후 자동 취소되기까지의 시간 — 아래 안내 문구("최대 4시간")와 맞춘다.
const RESERVATION_WINDOW_HOURS = 4;

function formatDateTime(date: Date) {
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  return `${date.getMonth() + 1}/${date.getDate()}(${WEEKDAY_KO[date.getDay()]}) ${hh}:${mm}`;
}

/**
 * 방금 예약/판매를 마치고 돌아온 경우 쿼리 파라미터를 사물함 목록에 반영한다.
 * 컴포넌트 state의 지연 초기값(lazy initial state)으로만 쓰인다 — 마운트 이후
 * search 가 바뀌어도 다시 계산하지 않는다.
 */
function applyHomeSearch(
  base: HomeLocker[],
  search: HomeSearch,
): HomeLocker[] {
  if (search.justReserved == null && search.justSold == null) return base;

  const now = new Date();

  if (search.justReserved != null) {
    const end = new Date(
      now.getTime() + RESERVATION_WINDOW_HOURS * 60 * 60 * 1000,
    );
    const reservedInfo = {
      product: search.product ?? "선택한 상품",
      period: `${formatDateTime(now)} ~ ${formatDateTime(end)}`,
      remaining: `${RESERVATION_WINDOW_HOURS}시간`,
    };
    return base.map((item) =>
      item.number === search.justReserved
        ? { ...item, status: "reserved", isMine: true, reservedInfo }
        : item,
    );
  }

  const expiry = new Date(
    now.getTime() + DEFAULT_MAX_OCCUPANCY_DAYS * 24 * 60 * 60 * 1000,
  );
  const sellingInfo = {
    product: search.product ?? "선택한 상품",
    price: search.price ?? "-",
    start: formatDateTime(now),
    expiry: formatDateTime(expiry),
    remaining: `${DEFAULT_MAX_OCCUPANCY_DAYS}일 남음`,
  };
  return base.map((item) =>
    item.number === search.justSold
      ? { ...item, status: "selling", isMine: true, sellingInfo }
      : item,
  );
}

type HomeLocker = LockerGridItem & {
  /** 본인이 예약/판매 중인 사물함인지 — 아니면 08-1 안내 모달 또는 토스트만 노출 */
  isMine?: boolean;
  /** 사물함 내부 규격 — 지정하지 않으면 DEFAULT_LOCKER_SIZE 를 쓴다 */
  size?: string;
  /** 최대 점유 가능 일수 — 지정하지 않으면 DEFAULT_MAX_OCCUPANCY_DAYS 를 쓴다 */
  maxOccupancyDays?: number;
  reservedInfo?: { product: string; period: string; remaining: string };
  sellingInfo?: {
    product: string;
    price: string;
    start: string;
    expiry: string;
    remaining: string;
  };
};

// 사물함이 전부 동일 규격이라 기본값으로 둔다 — 사물함마다 규격이 달라지면
// LOCKERS 각 항목에 size/maxOccupancyDays 를 지정해 덮어쓴다.
const DEFAULT_LOCKER_SIZE = "40 × 100 × 50 cm";
const DEFAULT_MAX_OCCUPANCY_DAYS = 7;

// Figma "01 홈 · 사물함 현황" 예시 데이터 — 실제 데이터는 API 연동 시 교체한다.
const LOCKERS: HomeLocker[] = [
  { number: 1, status: "empty" },
  { number: 2, status: "empty" },
  {
    number: 3,
    status: "selling",
    isMine: true,
    sellingInfo: {
      product: "골프채",
      price: "600,000원",
      start: "8/25(월) 15:10",
      expiry: "8/31(일) 15:10",
      remaining: "6일 남음",
    },
  },
  { number: 4, status: "empty" },
  {
    number: 5,
    status: "reserved",
    isMine: true,
    reservedInfo: {
      product: "골프채",
      period: "8/25(월) 15:10 ~ 8/25(월) 19:10",
      remaining: "3시간",
    },
  },
  { number: 6, status: "empty" },
  { number: 7, status: "empty" },
  { number: 8, status: "selling", isMine: false },
  { number: 9, status: "empty" },
  { number: 10, status: "selling", isMine: false },
  { number: 11, status: "empty" },
  { number: 12, status: "empty" },
  { number: 13, status: "reserved", isMine: false },
  { number: 14, status: "empty" },
  { number: 15, status: "empty" },
  { number: 16, status: "empty" },
];

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
 * "08-1 예약중입니다"). 로그인 게이트는 두지 않는다.
 * 데이터는 아직 정적 목데이터다 — API 연동 시 사물함 목록 쿼리로 교체한다.
 */
function HomePage() {
  const router = useRouter();
  const search = Route.useSearch();
  // search 는 최초 마운트 시 한 번만 반영한다 — 지연 초기값(lazy initial state)으로
  // 계산하고, 아래 effect 에서 쿼리 파라미터만 정리해 새로고침 시 재적용을 막는다.
  const [lockers, setLockers] = useState<HomeLocker[]>(() =>
    applyHomeSearch(LOCKERS, search),
  );
  const [sheet, setSheet] = useState<SheetState | null>(() => {
    if (search.justReserved == null) return null;
    const locker = applyHomeSearch(LOCKERS, search).find(
      (item) => item.number === search.justReserved,
    );
    return locker ? { type: "reserved", locker } : null;
  });
  const [infoModalLocker, setInfoModalLocker] = useState<HomeLocker | null>(
    null,
  );
  // "진열함 열기" 확인 모달 — 확인하면 예약중 → 판매중으로 전환된다.
  const [openingLocker, setOpeningLocker] = useState<HomeLocker | null>(null);

  const closeSheet = () => setSheet(null);

  // 사물함 예약 · 상품 선택 페이지에서 돌아온 직후라면 쿼리 파라미터를 지워
  // 새로고침/뒤로가기 시 같은 예약·판매 처리가 반복되지 않게 한다.
  useEffect(() => {
    if (search.justReserved == null && search.justSold == null) return;
    router.navigate({ to: "/", search: {}, replace: true });
  }, [search, router]);

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

  // "진열함 열기" 확인 — 예약중이던 사물함을 실제로 열어 판매중으로 전환한다.
  const confirmOpenLocker = () => {
    if (openingLocker) {
      const now = new Date();
      const expiry = new Date(
        now.getTime() + DEFAULT_MAX_OCCUPANCY_DAYS * 24 * 60 * 60 * 1000,
      );
      setLockers((prev) =>
        prev.map((item) =>
          item.number === openingLocker.number
            ? {
                ...item,
                status: "selling",
                isMine: true,
                reservedInfo: undefined,
                sellingInfo: {
                  product: openingLocker.reservedInfo?.product ?? "상품",
                  price: "-",
                  start: formatDateTime(now),
                  expiry: formatDateTime(expiry),
                  remaining: `${DEFAULT_MAX_OCCUPANCY_DAYS}일 남음`,
                },
              }
            : item,
        ),
      );
    }
    setOpeningLocker(null);
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

      <LockerGrid
        lockers={lockers}
        onSelect={handleSelect}
        className="px-4 pt-4"
      />

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
                  {sheet.locker.size ?? DEFAULT_LOCKER_SIZE} · 최대{" "}
                  {sheet.locker.maxOccupancyDays ?? DEFAULT_MAX_OCCUPANCY_DAYS}
                  일 점유 가능
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

          {sheet?.type === "reserved" && sheet.locker.reservedInfo && (
            <>
              <BottomSheetHeader className="h-auto items-center justify-start gap-2">
                <BottomSheetTitle>
                  {sheet.locker.number}번 사물함
                </BottomSheetTitle>
                <Badge variant="info">예약중</Badge>
              </BottomSheetHeader>
              <BottomSheetBody className="flex flex-col gap-3">
                <InfoBox
                  rows={[
                    {
                      label: "예약 상품",
                      value: sheet.locker.reservedInfo.product,
                    },
                    {
                      label: "점유 기간",
                      value: sheet.locker.reservedInfo.period,
                    },
                    {
                      label: "남은 시간",
                      value: sheet.locker.reservedInfo.remaining,
                    },
                  ]}
                />
                <p className="text-xs text-[var(--color-text-muted)]">
                  예약 후 최대 4시간 안에 상품을 넣지 않으면 예약이 자동
                  취소돼요.
                </p>
                <div className="flex gap-2 pt-2">
                  <Button
                    variant="secondary"
                    size="lg"
                    className="flex-1"
                    onClick={() => {
                      toast.success("예약이 취소됐어요.");
                      closeSheet();
                    }}
                  >
                    예약 취소
                  </Button>
                  <Button
                    size="lg"
                    className="flex-1"
                    onClick={() => {
                      setOpeningLocker(sheet.locker);
                      closeSheet();
                    }}
                  >
                    진열함 열기
                  </Button>
                </div>
              </BottomSheetBody>
            </>
          )}

          {sheet?.type === "selling" && sheet.locker.sellingInfo && (
            <>
              <BottomSheetHeader className="h-auto items-center justify-start gap-2">
                <BottomSheetTitle>
                  {sheet.locker.number}번 사물함
                </BottomSheetTitle>
                <Badge variant="danger">판매중</Badge>
              </BottomSheetHeader>
              <BottomSheetBody className="flex flex-col gap-3">
                <ItemRow
                  title={sheet.locker.sellingInfo.product}
                  place={sheet.locker.sellingInfo.price}
                  address="조회 24 · 찜 3"
                />
                <InfoBox
                  rows={[
                    {
                      label: "판매 시작",
                      value: sheet.locker.sellingInfo.start,
                    },
                    {
                      label: "점유 만료",
                      value: sheet.locker.sellingInfo.expiry,
                    },
                    {
                      label: "남은 점유 기간",
                      value: sheet.locker.sellingInfo.remaining,
                      tone: "danger",
                    },
                  ]}
                />
                <p className="text-xs text-[var(--color-text-muted)]">
                  판매를 종료하면 사물함이 열려요. 상품을 회수한 뒤 문을 닫아
                  주세요.
                </p>
                <Button
                  fullWidth
                  size="lg"
                  onClick={() => {
                    toast.success("판매가 종료됐어요.");
                    closeSheet();
                  }}
                >
                  판매 종료
                </Button>
              </BottomSheetBody>
            </>
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

      <Dialog
        open={openingLocker !== null}
        onOpenChange={(open) => !open && confirmOpenLocker()}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {openingLocker?.number}번 진열함이 열렸어요
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li>· 상품을 넣은 뒤 문을 닫으면 자동으로 잠겨요.</li>
            <li>
              · 점유 기간은 최대{" "}
              {openingLocker?.maxOccupancyDays ?? DEFAULT_MAX_OCCUPANCY_DAYS}
              일이에요.
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

          <Button fullWidth size="lg" onClick={confirmOpenLocker}>
            확인
          </Button>
        </DialogContent>
      </Dialog>
    </PageContainer>
  );
}
