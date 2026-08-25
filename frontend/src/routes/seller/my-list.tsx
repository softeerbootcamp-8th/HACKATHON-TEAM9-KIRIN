import { useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { cn } from "@/lib/utils";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { ProductCard } from "@/components/domain/product-card";
import { RegisterProductChip } from "@/components/domain/register-product-chip";

export const Route = createFileRoute("/seller/my-list")({
  component: MyListPage,
});

type ProductStatus = "예약중" | "판매중" | "판매대기" | "판매완료";

type Product = {
  id: string;
  name: string;
  price: number;
  status: ProductStatus;
  meta: string;
  highlight?: { text: string; tone: "info" | "danger" };
};

// 실제 데이터는 API 연동 시 사물함/거래 도메인 쿼리로 교체한다.
const PRODUCTS: Product[] = [
  {
    id: "wallet",
    name: "지갑",
    price: 170_000,
    status: "예약중",
    meta: "13번 사물함",
    highlight: { text: "2시간 50분 내 등록", tone: "info" },
  },
  {
    id: "golf-club",
    name: "골프채",
    price: 600_000,
    status: "판매중",
    meta: "16번 사물함 (8/25 15:10 - 8/31 15:10)",
    highlight: { text: "6일 남음", tone: "danger" },
  },
  {
    id: "earphone",
    name: "무선 이어폰",
    price: 45_000,
    status: "판매대기",
    meta: "사물함 미지정",
  },
  {
    id: "camping-chair",
    name: "캠핑 의자",
    price: 30_000,
    status: "판매완료",
    meta: "8/20 거래 완료",
  },
];

const STATUS_BADGE: Record<
  ProductStatus,
  { label: string; variant: "info" | "danger" | "muted" | "success" }
> = {
  예약중: { label: "예약중", variant: "info" },
  판매중: { label: "판매중", variant: "danger" },
  판매대기: { label: "판매대기", variant: "muted" },
  판매완료: { label: "판매완료", variant: "success" },
};

const TABS = [
  {
    key: "selling",
    label: "판매 중",
    statuses: ["예약중", "판매중", "판매대기"],
  },
  { key: "done", label: "거래 완료", statuses: ["판매완료"] },
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

  const tab = TABS.find((item) => item.key === activeTab) ?? TABS[0];
  const items = PRODUCTS.filter((product) =>
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
          {items.length === 0 ? (
            <p className="py-10 text-center text-sm text-[var(--color-text-muted)]">
              등록된 상품이 없습니다.
            </p>
          ) : (
            items.map((product) => (
              <ProductCard
                key={product.id}
                name={product.name}
                price={product.price}
                meta={product.meta}
                highlight={product.highlight}
                badge={STATUS_BADGE[product.status]}
              />
            ))
          )}
        </div>
      </div>
    </PageContainer>
  );
}
