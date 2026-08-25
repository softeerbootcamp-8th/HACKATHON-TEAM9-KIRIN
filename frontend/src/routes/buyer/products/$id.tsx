import { User } from "lucide-react";
import { createFileRoute, useRouter, Link } from "@tanstack/react-router";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/buyer/products/$id")({
  component: ProductDetailPage,
});

// QR 스캔으로 진입한 상품 상세 — 실제 데이터는 API 연동 시 상품 조회 쿼리로 교체한다.
const PRODUCT = {
  lockerNumber: 16,
  name: "골프채",
  price: 600_000,
  seller: {
    nickname: "판매자 4821",
    registeredAt: "8/25 15:10",
    dealCount: 12,
  },
  description:
    "작년에 구입한 캘러웨이 드라이버입니다. 라운딩 5회 사용했고 헤드 스크래치 없습니다.\n헤드커버 포함이며 직접 확인 후 가져가시면 됩니다.",
  storageLocation: "에테르노 청담 1층 로비",
  lockerSize: "16번 · 40 × 100 × 50 cm",
  dealDeadline: "8/31(일) 15:10까지",
};

/**
 * 상품 상세 (Figma "09 상품 상세 (QR 스캔 후)").
 * 헤더 타이틀은 상품명이 아니라 사물함 번호를 보여준다 — QR로 스캔한 사물함
 * 맥락을 유지하기 위한 디자인 의도로 보인다.
 */
function ProductDetailPage() {
  const router = useRouter();

  return (
    <PageContainer>
      <Header
        title={`${PRODUCT.lockerNumber}번 사물함`}
        onBack={() => router.history.back()}
      />

      {/* Figma 원본 상품 이미지 에셋이 없어 자리표시자로 대체한다 */}
      <div className="flex h-[220px] w-full items-center justify-center bg-[var(--color-surface-2)] text-sm text-[var(--color-text-muted)]">
        상품 이미지
      </div>

      <div className="flex flex-col gap-3.5 px-4 pt-3.5">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold text-[var(--color-text)]">
            {PRODUCT.name}
          </h1>
          <Badge variant="danger">판매중</Badge>
        </div>
        <p className="text-2xl font-bold text-[var(--color-text)]">
          {PRODUCT.price.toLocaleString()}원
        </p>

        <div className="h-px w-full bg-[var(--color-border)]" />

        <div className="flex items-center gap-2.5">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-[var(--color-surface-2)] text-[var(--color-text-muted)]">
            <User className="size-4" />
          </div>
          <div className="flex flex-col gap-0.5">
            <p className="text-sm font-medium text-[var(--color-text-sub)]">
              {PRODUCT.seller.nickname}
            </p>
            <p className="text-xs text-[var(--color-text-muted)]">
              {PRODUCT.seller.registeredAt} 등록 · 거래{" "}
              {PRODUCT.seller.dealCount}회
            </p>
          </div>
        </div>

        <div className="h-px w-full bg-[var(--color-border)]" />

        <p className="text-[13px] whitespace-pre-line text-[var(--color-text-sub)]">
          {PRODUCT.description}
        </p>

        <div className="flex w-full flex-col gap-2.5 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-2)] p-3.5 text-[13px]">
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">보관 위치</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {PRODUCT.storageLocation}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">사물함</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {PRODUCT.lockerSize}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--color-text-muted)]">거래 기한</span>
            <span className="font-medium text-[var(--color-text-sub)]">
              {PRODUCT.dealDeadline}
            </span>
          </div>
        </div>
      </div>

      <div className="mt-auto flex items-center gap-3 border-t border-[var(--color-border)] px-4 py-3">
        <div className="flex flex-col gap-0.5">
          <p className="text-[11px] text-[var(--color-text-muted)]">
            결제 금액
          </p>
          <p className="text-[17px] font-bold text-[var(--color-text-sub)]">
            {PRODUCT.price.toLocaleString()}원
          </p>
        </div>
        <Button asChild size="lg" className="flex-1">
          <Link to="/buyer/checkout">구매하기</Link>
        </Button>
      </div>
    </PageContainer>
  );
}
