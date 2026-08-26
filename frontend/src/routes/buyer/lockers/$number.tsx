import { User } from "lucide-react";
import { createFileRoute, redirect, useRouter, Link } from "@tanstack/react-router";
import axios from "axios";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PhotoCarousel } from "@/components/domain/photo-carousel";
import { formatDateTime, formatPrice } from "@/lib/format";
import {
  useGetProductByLocker,
  getGetProductByLockerQueryOptions,
} from "@/api/generated/lockers/lockers";
import type { ErrorResponse, ProductStatus } from "@/api/generated/model";

function isEmptyLockerError(error: unknown) {
  return (
    axios.isAxiosError<ErrorResponse>(error) && error.response?.status === 404
  );
}

export const Route = createFileRoute("/buyer/lockers/$number")({
  component: ProductDetailPage,
  // 빈 진열함이면 홈 화면 + 빈 진열함 바텀시트로 보여줘야 해서, 라우트가 마운트되기
  // 전에(=상품 상세 화면이 잠깐이라도 그려지기 전에) 여기서 먼저 확인하고 홈으로
  // 리다이렉트한다. 이렇게 하면 로딩 중 "상품 상세" 페이지가 깜빡이지 않는다.
  loader: async ({ params, context }) => {
    const lockerId = Number(params.number);
    try {
      await context.queryClient.ensureQueryData(
        getGetProductByLockerQueryOptions(lockerId),
      );
    } catch (error) {
      if (isEmptyLockerError(error)) {
        throw redirect({
          to: "/",
          search: { openLocker: lockerId },
          replace: true,
        });
      }
      // 그 외 에러(네트워크 오류 등)는 컴포넌트가 기존 방식대로 처리한다.
    }
  },
});

const STATUS_BADGE: Record<
  ProductStatus,
  { label: string; variant: "info" | "danger" | "muted" | "success" }
> = {
  PREPARING: { label: "준비중", variant: "muted" },
  RESERVED: { label: "예약중", variant: "info" },
  SELLING: { label: "판매중", variant: "danger" },
  SOLD: { label: "판매완료", variant: "success" },
  EXPIRED: { label: "판매만료", variant: "muted" },
};

/**
 * 상품 상세 (Figma "09 상품 상세 (QR 스캔 후)").
 * 진열함에 붙은 QR을 스캔해 들어오는 화면이라 상품 id가 아니라 진열함 번호로
 * 접근한다 — `GET /lockers/{lockerId}/product`로 그 진열함에 지금 있는 상품을 조회한다.
 * 헤더 타이틀도 상품명이 아니라 진열함 번호를 보여준다.
 */
function ProductDetailPage() {
  const router = useRouter();
  const { number } = Route.useParams();
  const lockerId = Number(number);

  const { data: product, isLoading, error } = useGetProductByLocker(lockerId);

  if (isLoading) {
    return (
      <PageContainer>
        <Header title="상품 상세" onBack={() => router.navigate({ to: "/" })} />
        <p className="px-4 pt-10 text-center text-sm text-[var(--color-text-muted)]">
          불러오는 중...
        </p>
      </PageContainer>
    );
  }

  if (error || !product) {
    return (
      <PageContainer>
        <Header title="상품 상세" onBack={() => router.navigate({ to: "/" })} />
        <p className="px-4 pt-10 text-center text-sm text-[var(--color-text-muted)]">
          정보를 불러오지 못했어요.
        </p>
      </PageContainer>
    );
  }

  const badge = STATUS_BADGE[product.status];
  const isPurchasable = product.status === "SELLING";

  return (
    <PageContainer>
      <Header
        title={`${lockerId}번 진열함`}
        onBack={() => router.navigate({ to: "/" })}
      />

      <PhotoCarousel imageUrls={product.imageUrls} alt={product.name} />

      <div className="flex flex-col gap-3.5 px-4 pt-3.5">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold text-[var(--color-text)]">
            {product.name}
          </h1>
          <Badge variant={badge.variant}>{badge.label}</Badge>
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
              {product.sellerName}
            </p>
            <p className="text-xs text-[var(--color-text-muted)]">
              {formatDateTime(product.createdAt)} 등록
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
            <span className="text-[var(--color-text-muted)]">진열함</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {lockerId}번
            </span>
          </div>
          {product.sellingExpiresAt && (
            <div className="flex items-center justify-between">
              <span className="text-[var(--color-text-muted)]">거래 기한</span>
              <span className="font-medium text-[var(--color-text-sub)]">
                {formatDateTime(product.sellingExpiresAt)}까지
              </span>
            </div>
          )}
        </div>
      </div>

      <div className="mt-auto flex items-center gap-3 border-t border-[var(--color-border)] px-4 py-3">
        <div className="flex flex-col gap-0.5">
          <p className="text-[11px] text-[var(--color-text-muted)]">
            결제 금액
          </p>
          <p className="text-[17px] font-bold text-[var(--color-text-sub)]">
            {formatPrice(product.price)}
          </p>
        </div>
        {isPurchasable ? (
          <Button asChild size="lg" className="flex-1">
            <Link
              to="/buyer/checkout"
              search={{ productId: product.productId }}
            >
              구매하기
            </Link>
          </Button>
        ) : (
          <Button size="lg" className="flex-1" disabled>
            {badge.label}
          </Button>
        )}
      </div>
    </PageContainer>
  );
}
