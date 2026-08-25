import { useState } from "react";
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

export const Route = createFileRoute("/")({
  component: HomePage,
});

type HomeLocker = LockerGridItem & {
  /** 본인이 예약/판매 중인 사물함인지 — 아니면 08-1 안내 모달 또는 토스트만 노출 */
  isMine?: boolean;
  reservedInfo?: { product: string; period: string; remaining: string };
  sellingInfo?: {
    product: string;
    price: string;
    start: string;
    expiry: string;
    remaining: string;
  };
};

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
  const [sheet, setSheet] = useState<SheetState | null>(null);
  const [infoModalLocker, setInfoModalLocker] = useState<HomeLocker | null>(
    null,
  );

  const closeSheet = () => setSheet(null);

  const handleSelect = (number: number) => {
    const locker = LOCKERS.find((item) => item.number === number);
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
        lockers={LOCKERS}
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
                <p className="text-sm text-[var(--color-text-muted)]">
                  등록된 상품이 없습니다.
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
                      toast.success("사물함이 열렸어요.");
                      closeSheet();
                    }}
                  >
                    사물함 열기
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
    </PageContainer>
  );
}
