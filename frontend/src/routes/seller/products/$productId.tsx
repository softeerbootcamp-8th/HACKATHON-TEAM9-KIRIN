import { Pencil, User } from "lucide-react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { Badge } from "@/components/ui/badge";
import { PhotoCarousel } from "@/components/domain/photo-carousel";
import { formatDateTime, formatPrice } from "@/lib/format";
import { useGetProduct } from "@/api/generated/products/products";
import type { ProductStatus } from "@/api/generated/model";

export const Route = createFileRoute("/seller/products/$productId")({
  component: SellerProductDetailPage,
});

const STATUS_BADGE: Record<
  ProductStatus,
  { label: string; variant: "info" | "danger" | "muted" | "success" }
> = {
  PREPARING: { label: "판매대기", variant: "muted" },
  RESERVED: { label: "예약중", variant: "info" },
  SELLING: { label: "판매중", variant: "danger" },
  SOLD: { label: "판매완료", variant: "success" },
  EXPIRED: { label: "판매만료", variant: "muted" },
};

/** 지금은 보관 장소가 한 곳뿐이라 고정 문구로 보여준다. */
const STORAGE_LOCATION = "에테르노 청담 1층 로비";

/**
 * 판매자 본인 상품 상세. 내 리스트, 홈 예약중·판매중 바텀시트에서 상품 하나를
 * 골라 들어오는 화면이라 productId로 조회한다. 구매자용 상세("09 상품 상세")와
 * 달리 구매 CTA 대신 거래 횟수 등 판매자에게만 의미 있는 정보를 보여준다.
 */
function SellerProductDetailPage() {
  const router = useRouter();
  const { productId } = Route.useParams();

  const { data: product, isLoading } = useGetProduct(Number(productId));

  if (isLoading || !product) {
    return (
      <PageContainer>
        <Header title="상품 상세" onBack={() => router.history.back()} />
        <p className="px-4 pt-10 text-center text-sm text-[var(--color-text-muted)]">
          불러오는 중...
        </p>
      </PageContainer>
    );
  }

  const badge = STATUS_BADGE[product.status];
  const expiresAt =
    product.status === "RESERVED"
      ? product.reservationExpiresAt
      : product.status === "SELLING"
        ? product.sellingExpiresAt
        : null;
  const expiresLabel =
    product.status === "RESERVED" ? "예약 만료" : "거래 기한";

  return (
    <PageContainer>
      <Header
        title={product.lockerId ? `${product.lockerId}번 진열함` : "상품 상세"}
        onBack={() => router.history.back()}
      />

      <PhotoCarousel imageUrls={product.imageUrls} alt={product.name} />

      <div className="flex flex-col gap-3.5 px-4 pt-3.5">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold text-[var(--color-text)]">
            {product.name}
          </h1>
          <Badge variant={badge.variant}>{badge.label}</Badge>
          {product.status !== "SOLD" && (
            <button
              type="button"
              onClick={() =>
                router.navigate({
                  to: "/seller/products/new",
                  search: { productId: product.productId },
                })
              }
              className="ml-auto flex items-center gap-1 text-xs text-[var(--color-text-muted)]"
            >
              <Pencil className="size-3" />
              수정하기
            </button>
          )}
        </div>
        <p className="text-2xl font-bold text-[var(--color-text)]">
          {formatPrice(product.price)}
        </p>

        <div className="h-px w-full bg-[var(--color-border)]" />

        <div className="flex items-center gap-2.5">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-[var(--color-surface-2)] text-[var(--color-text-muted)]">
            <User className="size-4" />
          </div>
          <div className="flex flex-col gap-0.5">
            <p className="text-sm font-medium text-[var(--color-text-sub)]">
              판매자 {product.sellerName}
            </p>
            <p className="text-xs text-[var(--color-text-muted)]">
              {formatDateTime(product.createdAt)} 등록
              {product.sellerCompletedSalesCount != null &&
                ` · 거래 ${product.sellerCompletedSalesCount}회`}
            </p>
          </div>
        </div>

        <div className="h-px w-full bg-[var(--color-border)]" />

        {product.description && (
          <p className="text-[13px] whitespace-pre-line text-[var(--color-text-sub)]">
            {product.description}
          </p>
        )}

        <div className="flex w-full flex-col gap-2.5 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-2)] p-3.5 text-[13px]">
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">보관 위치</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {STORAGE_LOCATION}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">진열함</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {product.lockerId ? `${product.lockerId}번` : "미배치"}
            </span>
          </div>
          {expiresAt && (
            <div className="flex items-center justify-between">
              <span className="text-[var(--color-text-muted)]">
                {expiresLabel}
              </span>
              <span className="font-medium text-[var(--color-text-sub)]">
                {formatDateTime(expiresAt)}까지
              </span>
            </div>
          )}
        </div>
      </div>
    </PageContainer>
  );
}
