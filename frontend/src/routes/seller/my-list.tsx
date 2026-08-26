import { useState } from "react";
import { createFileRoute, useRouter, Link } from "@tanstack/react-router";
import { cn } from "@/lib/utils";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { ProductCard } from "@/components/domain/product-card";
import { RegisterProductChip } from "@/components/domain/register-product-chip";
import {
  formatDateTime,
  formatRemaining,
  formatRemainingDetailed,
  formatShortDate,
} from "@/lib/format";
import { useGetMyProducts } from "@/api/generated/products/products";
import type {
  ProductStatus,
  ProductSummaryResponse,
} from "@/api/generated/model";

export const Route = createFileRoute("/seller/my-list")({
  component: MyListPage,
});

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

/**
 * 내 리스트 (Figma "07 내 리스트 · 판매 중" / "07-2 내 리스트 · 거래 완료").
 * 두 화면은 필터 칩으로 전환되는 같은 화면의 두 상태라 하나의 라우트로 합쳤다.
 */
function MyListPage() {
  const router = useRouter();
  const [activeTab, setActiveTab] =
    useState<(typeof TABS)[number]["key"]>("selling");

  const { data, isLoading } = useGetMyProducts({});
  const allProducts = data?.products ?? [];

  const tab = TABS.find((item) => item.key === activeTab) ?? TABS[0];
  const items = allProducts.filter((product) =>
    (tab.statuses as readonly ProductStatus[]).includes(product.status),
  );

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
                />
              </Link>
            ))
          )}
        </div>
      </div>
    </PageContainer>
  );
}
