import { useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { ItemRow } from "@/components/domain/item-row";
import { RegisterProductChip } from "@/components/domain/register-product-chip";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";

export const Route = createFileRoute("/seller/lockers/$number/reserve")({
  component: ReservePage,
});

// 아직 사물함에 배치되지 않은 내 등록 상품 — 실제 데이터는 API 연동 시 교체한다.
const MY_UNPLACED_PRODUCTS = [
  {
    id: "golf-club",
    title: "골프채",
    place: "600,000원",
    address: "등록 8/24",
  },
  { id: "wallet", title: "지갑", place: "170,000원", address: "등록 8/20" },
  {
    id: "earphone",
    title: "무선 이어폰",
    place: "45,000원",
    address: "등록 8/18",
  },
];

/**
 * 사물함 예약 · 상품 선택 (Figma "05 사물함 예약 · 상품 선택").
 * 예약 확정 시 "08 모달 · 사물함 잠금 해제" 를 띄우고, 확인하면 홈으로 돌아간다.
 */
function ReservePage() {
  const { number } = Route.useParams();
  const router = useRouter();
  // 사물함 한 칸에는 상품을 하나만 넣을 수 있어 단일 선택으로 동작한다.
  const [selectedId, setSelectedId] = useState<string | null>(
    MY_UNPLACED_PRODUCTS[0].id,
  );
  const [unlocked, setUnlocked] = useState(false);

  return (
    <PageContainer>
      <Header
        title={`${number}번 사물함 예약`}
        onBack={() => router.history.back()}
      />

      <div className="flex flex-col gap-2.5 px-4 pt-2">
        <div className="flex items-center justify-between">
          <h2 className="text-[15px] font-bold text-[var(--color-text-sub)]">
            판매할 상품 선택
          </h2>
          <span className="text-xs text-[var(--color-text-muted)]">
            {selectedId ? 1 : 0}개 선택됨
          </span>
        </div>

        <RegisterProductChip />

        <div className="flex flex-col gap-2.5">
          {MY_UNPLACED_PRODUCTS.map((product) => (
            <ItemRow
              key={product.id}
              title={product.title}
              place={product.place}
              address={product.address}
              selectable
              checked={selectedId === product.id}
              onCheckedChange={(checked) =>
                setSelectedId(checked ? product.id : null)
              }
            />
          ))}
        </div>

        <p className="text-xs text-[var(--color-text-muted)]">
          선택한 상품은 예약 확정 후 {number}번 사물함에 등록돼요.
        </p>
      </div>

      <div className="mt-auto flex flex-col border-t border-[var(--color-border)] px-4 py-3">
        <Button
          fullWidth
          size="lg"
          disabled={!selectedId}
          onClick={() => setUnlocked(true)}
        >
          예약 확정하기
        </Button>
      </div>

      <Dialog
        open={unlocked}
        onOpenChange={(open) => {
          if (!open) router.navigate({ to: "/" });
        }}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {number}번 사물함이 열렸어요
          </DialogTitle>

          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li>· 상품을 넣은 뒤 문을 닫으면 자동으로 잠겨요.</li>
            <li>
              · 점유 기간은 최대 7일이에요.
              <br />
              <span className="font-bold text-[var(--color-text-sub)]">
                오늘부터 7일 후 자동 만료
              </span>
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
            onClick={() => router.navigate({ to: "/" })}
          >
            확인
          </Button>
        </DialogContent>
      </Dialog>
    </PageContainer>
  );
}
