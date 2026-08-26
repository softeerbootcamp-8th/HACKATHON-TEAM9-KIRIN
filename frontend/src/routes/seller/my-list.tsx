import { useState } from "react";
import { createFileRoute, useRouter, Link } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { MoreVertical } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { ProductCard } from "@/components/domain/product-card";
import { RegisterProductChip } from "@/components/domain/register-product-chip";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from "@/components/ui/dropdown-menu";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import {
  formatDateTime,
  formatDuration,
  formatOccupancyPeriod,
  formatPrice,
  formatRemaining,
  formatRemainingDetailed,
  formatShortDate,
  josaEulReul,
} from "@/lib/format";
import {
  useGetMyProducts,
  useCancelLockerReservation,
  useStartDeposit,
  useStartRecovery,
  useDeleteProduct,
  getGetMyProductsQueryKey,
} from "@/api/generated/products/products";
import {
  useChangeLockStatus,
  getGetLockersQueryKey,
} from "@/api/generated/lockers/lockers";
import type {
  ProductStatus,
  ProductSummaryResponse,
} from "@/api/generated/model";

export const Route = createFileRoute("/seller/my-list")({
  component: MyListPage,
});

// DEFAULT_MAX_OCCUPANCY_DAYS(src/routes/index.tsx)와 동일한 값(백엔드 SELLING_DAYS).
const MAX_OCCUPANCY_DAYS = 7;

const STATUS_BADGE: Record<
  ProductStatus,
  {
    label: string;
    variant: "info" | "danger" | "muted" | "mine" | "mineSelling";
  }
> = {
  RESERVED: { label: "예약중", variant: "mine" },
  SELLING: { label: "판매중", variant: "mineSelling" },
  PREPARING: { label: "판매대기", variant: "muted" },
  SOLD: { label: "판매완료", variant: "muted" },
  EXPIRED: { label: "판매만료", variant: "muted" },
};

/** 진열함/보조 정보 한 줄 — 상태별로 보여줄 내용이 다르다 (Figma "07"/"07-2"). */
function getMeta(product: ProductSummaryResponse): string {
  if (product.status === "SOLD" && product.soldAt) {
    return `${formatShortDate(product.soldAt)} 거래 완료`;
  }
  if (product.lockerId == null) {
    return "진열함 미지정";
  }
  if (
    product.status === "SELLING" &&
    product.sellingStartedAt &&
    product.sellingExpiresAt
  ) {
    return `${product.lockerId}번 진열함 (${formatDateTime(product.sellingStartedAt)} - ${formatDateTime(product.sellingExpiresAt)})`;
  }
  return `${product.lockerId}번 진열함`;
}

/** 남은 예약 시간(예약중)/남은 판매 기간(판매중) 강조 캡션 (Figma "07"). */
function getHighlight(product: ProductSummaryResponse) {
  if (product.status === "RESERVED" && product.reservationExpiresAt) {
    return {
      text: formatRemainingDetailed(product.reservationExpiresAt),
      tone: "mine" as const,
    };
  }
  if (product.status === "SELLING" && product.sellingExpiresAt) {
    return {
      text: formatRemaining(product.sellingExpiresAt),
      tone: "selling" as const,
    };
  }
  return undefined;
}

const TABS = [
  {
    key: "selling",
    label: "판매 중",
    statuses: ["RESERVED", "SELLING", "PREPARING"],
  },
  { key: "done", label: "거래 완료", statuses: ["SOLD", "EXPIRED"] },
] as const satisfies {
  key: string;
  label: string;
  statuses: ProductStatus[];
}[];

type ProductMoreMenuProps = {
  product: ProductSummaryResponse;
  onEdit: (product: ProductSummaryResponse) => void;
  onStartDeposit: (product: ProductSummaryResponse) => void;
  onRequestCancelReservation: (product: ProductSummaryResponse) => void;
  onEndSelling: (product: ProductSummaryResponse) => void;
  onRequestDelete: (product: ProductSummaryResponse) => void;
};

/**
 * 상품 카드 우측 상단 점 세개(더보기) 메뉴. 상태별로 항목이 달라진다 (Figma "07 내 리스트 · 판매 중").
 * - 예약중: 상품 수정 / 물건 넣기 / 예약 취소
 * - 판매중: 상품 수정 / 판매 중단
 * - 판매대기: 상품 수정 / 삭제
 */
function ProductMoreMenu({
  product,
  onEdit,
  onStartDeposit,
  onRequestCancelReservation,
  onEndSelling,
  onRequestDelete,
}: ProductMoreMenuProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label="더보기"
          className="shrink-0 text-[var(--color-text-muted)]"
          onClick={(event) => {
            // 카드 전체가 <Link>로 감싸여 있어, 막지 않으면 클릭이 상세 화면
            // 이동으로 이어진다.
            event.preventDefault();
            event.stopPropagation();
          }}
        >
          <MoreVertical className="size-6" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={() => onEdit(product)}>
          상품 수정
        </DropdownMenuItem>
        {product.status === "RESERVED" && (
          <>
            <DropdownMenuItem onClick={() => onStartDeposit(product)}>
              물건 넣기
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() => onRequestCancelReservation(product)}
            >
              예약 취소
            </DropdownMenuItem>
          </>
        )}
        {product.status === "SELLING" && (
          <DropdownMenuItem onClick={() => onEndSelling(product)}>
            판매 중단
          </DropdownMenuItem>
        )}
        {product.status === "PREPARING" && (
          <DropdownMenuItem onClick={() => onRequestDelete(product)}>
            삭제
          </DropdownMenuItem>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/**
 * 내 리스트 (Figma "07 내 리스트 · 판매 중" / "07-2 내 리스트 · 거래 완료").
 * 두 화면은 필터 칩으로 전환되는 같은 화면의 두 상태라 하나의 라우트로 합쳤다.
 */
function MyListPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] =
    useState<(typeof TABS)[number]["key"]>("selling");

  // 08 모달 · 사물함 잠금 해제 — 물건 넣기 직후 안내(단일 버튼).
  const [depositInfo, setDepositInfo] = useState<{
    product: ProductSummaryResponse;
    depositedAt: Date;
  } | null>(null);
  // 08-2 모달 · 판매 종료 — 판매 중단 직후 안내(단일 버튼).
  const [endSellingInfo, setEndSellingInfo] =
    useState<ProductSummaryResponse | null>(null);
  // 08-3 모달 · 예약 취소 — 실행 전 확인(버튼 2개).
  const [cancelTarget, setCancelTarget] =
    useState<ProductSummaryResponse | null>(null);
  // 08-4 모달 · 상품 삭제 — 실행 전 확인(버튼 2개).
  const [deleteTarget, setDeleteTarget] =
    useState<ProductSummaryResponse | null>(null);

  const { data, isLoading } = useGetMyProducts({});
  const allProducts = data?.products ?? [];

  const tab = TABS.find((item) => item.key === activeTab) ?? TABS[0];
  const items = allProducts.filter((product) =>
    (tab.statuses as readonly ProductStatus[]).includes(product.status),
  );

  const changeLockStatus = useChangeLockStatus();
  const startDeposit = useStartDeposit();
  const startRecovery = useStartRecovery();
  const cancelReservation = useCancelLockerReservation();
  const deleteProduct = useDeleteProduct();

  const invalidateProductData = () => {
    queryClient.invalidateQueries({ queryKey: getGetMyProductsQueryKey({}) });
    queryClient.invalidateQueries({ queryKey: getGetLockersQueryKey() });
  };

  const handleEdit = (product: ProductSummaryResponse) => {
    router.navigate({
      to: "/seller/products/new",
      search: { productId: product.productId },
    });
  };

  /** 물건 넣기: 사물함을 열고 투입을 시작한 뒤, 08 모달로 안내한다. */
  const handleStartDeposit = (product: ProductSummaryResponse) => {
    if (product.lockerId == null) return;
    changeLockStatus.mutate(
      { lockerId: product.lockerId, data: { lockStatus: "UNLOCKED" } },
      {
        onSuccess: () => {
          startDeposit.mutate(
            { productId: product.productId },
            {
              onSuccess: (updated) => {
                invalidateProductData();
                setDepositInfo({
                  product,
                  depositedAt: updated.depositStartedAt
                    ? new Date(updated.depositStartedAt)
                    : new Date(),
                });
              },
              onError: () => toast.error("입고 처리에 실패했어요."),
            },
          );
        },
        onError: () => toast.error("진열함을 여는 데 실패했어요."),
      },
    );
  };

  /** 판매 중단: 사물함을 열고 회수를 시작한 뒤, 08-2 모달로 안내한다. */
  const handleEndSelling = (product: ProductSummaryResponse) => {
    if (product.lockerId == null) return;
    changeLockStatus.mutate(
      { lockerId: product.lockerId, data: { lockStatus: "UNLOCKED" } },
      {
        onSuccess: () => {
          startRecovery.mutate(
            { productId: product.productId },
            {
              onSuccess: () => {
                invalidateProductData();
                setEndSellingInfo(product);
              },
              onError: () => toast.error("판매 종료 처리에 실패했어요."),
            },
          );
        },
        onError: () => toast.error("진열함을 여는 데 실패했어요."),
      },
    );
  };

  /** 08-3 모달의 "예약 취소" 버튼 — 여기서만 실제로 취소 API를 호출한다. */
  const handleConfirmCancelReservation = () => {
    if (!cancelTarget) return;
    cancelReservation.mutate(
      { productId: cancelTarget.productId },
      {
        onSuccess: () => {
          toast.success("예약이 취소됐어요.");
          invalidateProductData();
          setCancelTarget(null);
        },
        onError: () => toast.error("예약 취소에 실패했어요."),
      },
    );
  };

  /** 08-4 모달의 "삭제" 버튼 — 여기서만 실제로 삭제 API를 호출한다. */
  const handleConfirmDelete = () => {
    if (!deleteTarget) return;
    deleteProduct.mutate(
      { productId: deleteTarget.productId },
      {
        onSuccess: () => {
          toast.success("상품을 삭제했어요.");
          invalidateProductData();
          setDeleteTarget(null);
        },
        onError: () => toast.error("상품 삭제에 실패했어요."),
      },
    );
  };

  return (
    <PageContainer>
      <Header title="내 리스트" onBack={() => router.history.back()} />

      <div className="flex flex-col gap-2.5 px-4 pt-2">
        <div className="flex gap-2">
          {TABS.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => setActiveTab(item.key)}
              className={cn(
                "rounded-[var(--radius-pill)] border px-4 py-2 text-[13px]",
                activeTab === item.key
                  ? "border-[var(--color-primary)] bg-[var(--color-primary)] font-medium text-white"
                  : "border-[var(--color-border)] bg-[var(--color-bg)] text-[var(--color-text-muted)]",
              )}
            >
              {item.label}
            </button>
          ))}
        </div>

        <div className="flex items-center justify-between pt-1">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            등록한 상품
          </h2>
          <span className="text-xs text-[var(--color-text-muted)]">
            {items.length}개
          </span>
        </div>

        <RegisterProductChip />

        <div className="flex flex-col gap-2.5">
          {isLoading ? (
            <p className="py-10 text-center text-sm text-[var(--color-text-muted)]">
              불러오는 중...
            </p>
          ) : items.length === 0 ? (
            <p className="py-10 text-center text-sm text-[var(--color-text-muted)]">
              등록된 상품이 없습니다.
            </p>
          ) : (
            items.map((product) => (
              <Link
                key={product.productId}
                to="/seller/products/$productId"
                params={{ productId: String(product.productId) }}
              >
                <ProductCard
                  name={product.name}
                  price={product.price}
                  meta={getMeta(product)}
                  highlight={getHighlight(product)}
                  badge={STATUS_BADGE[product.status]}
                  thumbnailUrl={product.imageUrl ?? undefined}
                  moreMenu={
                    tab.key === "selling" ? (
                      <ProductMoreMenu
                        product={product}
                        onEdit={handleEdit}
                        onStartDeposit={handleStartDeposit}
                        onRequestCancelReservation={setCancelTarget}
                        onEndSelling={handleEndSelling}
                        onRequestDelete={setDeleteTarget}
                      />
                    ) : undefined
                  }
                />
              </Link>
            ))
          )}
        </div>
      </div>

      {/* 08 모달 · 사물함 잠금 해제 (내 리스트) */}
      <Dialog
        open={depositInfo !== null}
        onOpenChange={(open) => !open && setDepositInfo(null)}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {depositInfo?.product.lockerId}번 진열함이 열렸어요
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
                {depositInfo && (
                  <p className="font-bold">
                    {formatOccupancyPeriod(
                      depositInfo.depositedAt,
                      MAX_OCCUPANCY_DAYS,
                    )}
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

          <Button fullWidth size="lg" onClick={() => setDepositInfo(null)}>
            확인
          </Button>
        </DialogContent>
      </Dialog>

      {/* 08-2 모달 · 판매 종료 (내 리스트) */}
      <Dialog
        open={endSellingInfo !== null}
        onOpenChange={(open) => !open && setEndSellingInfo(null)}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {endSellingInfo?.lockerId}번 진열함 판매를 종료했어요
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <p className="flex-1">
                진열함 문이 열렸어요. 상품을 회수해 주세요.
              </p>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <div className="flex-1">
                <p>판매를 종료한 상품이에요.</p>
                {endSellingInfo && (
                  <p className="font-bold">
                    {endSellingInfo.name} / {formatPrice(endSellingInfo.price)}
                  </p>
                )}
              </div>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <p className="flex-1">문을 닫으면 진열함 점유가 끝나요.</p>
            </li>
          </ul>

          <div className="flex gap-1.5 rounded-[var(--radius-sm)] bg-[var(--color-primary-weak)] p-3 text-[var(--color-primary)]">
            <span className="text-[13px] font-bold">!</span>
            <p className="text-xs font-medium">
              상품을 두고 닫으면 점유가 계속돼요.
              <br />
              반드시 상품을 회수한 뒤 닫아 주세요.
            </p>
          </div>

          <Button fullWidth size="lg" onClick={() => setEndSellingInfo(null)}>
            확인
          </Button>
        </DialogContent>
      </Dialog>

      {/* 08-3 모달 · 예약 취소 (내 리스트) */}
      <Dialog
        open={cancelTarget !== null}
        onOpenChange={(open) => !open && setCancelTarget(null)}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {cancelTarget?.lockerId}번 진열함 예약을 취소할까요?
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <p className="flex-1">
                예약이 풀리면 다른 사용자가 예약할 수 있어요.
              </p>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <div className="flex-1">
                <p>예약한 상품이에요.</p>
                {cancelTarget && (
                  <p className="font-bold">
                    {cancelTarget.name} / {formatPrice(cancelTarget.price)}
                  </p>
                )}
              </div>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <p className="flex-1">
                남은 예약 시간{" "}
                {cancelTarget?.reservationExpiresAt
                  ? formatDuration(cancelTarget.reservationExpiresAt)
                  : ""}
                은 사라져요.
              </p>
            </li>
          </ul>

          <div className="flex gap-1.5 rounded-[var(--radius-sm)] bg-[var(--color-primary-weak)] p-3 text-[var(--color-primary)]">
            <span className="text-[13px] font-bold">!</span>
            <p className="text-xs font-medium">
              취소하면 되돌릴 수 없어요.
              <br />
              다시 예약하려면 처음부터 진행해야 해요.
            </p>
          </div>

          <div className="flex gap-2">
            <Button
              fullWidth
              size="lg"
              variant="secondary"
              onClick={() => setCancelTarget(null)}
            >
              돌아가기
            </Button>
            <Button
              fullWidth
              size="lg"
              disabled={cancelReservation.isPending}
              onClick={handleConfirmCancelReservation}
            >
              예약 취소
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* 08-4 모달 · 상품 삭제 (내 리스트) */}
      <Dialog
        open={deleteTarget !== null}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {deleteTarget?.name}
            {deleteTarget ? josaEulReul(deleteTarget.name) : ""} 삭제할까요?
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <p className="flex-1">등록한 상품 목록에서 완전히 사라져요.</p>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <div className="flex-1">
                <p>삭제할 상품이에요.</p>
                {deleteTarget && (
                  <p className="font-bold">
                    {deleteTarget.name} / {formatPrice(deleteTarget.price)}
                  </p>
                )}
              </div>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="font-bold">·</span>
              <p className="flex-1">진열함에 없는 상품이라 바로 삭제돼요.</p>
            </li>
          </ul>

          <div className="flex gap-1.5 rounded-[var(--radius-sm)] bg-[var(--color-primary-weak)] p-3 text-[var(--color-primary)]">
            <span className="text-[13px] font-bold">!</span>
            <p className="text-xs font-medium">
              삭제하면 되돌릴 수 없어요.
              <br />
              같은 상품은 다시 등록해야 해요.
            </p>
          </div>

          <div className="flex gap-2">
            <Button
              fullWidth
              size="lg"
              variant="secondary"
              onClick={() => setDeleteTarget(null)}
            >
              돌아가기
            </Button>
            <Button
              fullWidth
              size="lg"
              disabled={deleteProduct.isPending}
              onClick={handleConfirmDelete}
            >
              삭제
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </PageContainer>
  );
}
