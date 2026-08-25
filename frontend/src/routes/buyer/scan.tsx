import { useState } from "react";
import { createFileRoute, useRouter, Link } from "@tanstack/react-router";
import { X } from "lucide-react";
import { PageContainer } from "@/components/layout/page";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";

export const Route = createFileRoute("/buyer/scan")({
  component: ScanPage,
});

// ScanFrame 모서리 브래킷 — 코너마다 가로/세로 막대 2개씩 (Figma "Corner")
const SCAN_FRAME_CORNERS = [
  "top-[-1px] left-[-1px] h-1 w-10",
  "top-[-1px] left-[-1px] h-10 w-1",
  "top-[-1px] right-[-1px] h-1 w-10",
  "top-[-1px] right-[-1px] h-10 w-1",
  "bottom-[-1px] left-[-1px] h-1 w-10",
  "bottom-[-1px] left-[-1px] h-10 w-1",
  "bottom-[-1px] right-[-1px] h-1 w-10",
  "bottom-[-1px] right-[-1px] h-10 w-1",
] as const;

/**
 * QR 스캔 (Figma "12 QR 스캔 · 모달(예약중)").
 * 카메라 프리뷰는 실제 연동 전이라 어두운 배경 + 스캔 프레임만 그린다. 이 화면은
 * 라이트 테마 토큰을 쓰지 않는 유일한 예외(카메라 뷰 특성상 항상 어둡다).
 *
 * Figma 프레임 자체가 "예약중 안내 모달이 스캔 화면 위에 뜬 상태"를 그대로
 * 담고 있어, 진입 시 모달이 기본으로 열려 있다. 모달을 닫으면 스캔 프레임을
 * 눌러 사물함 번호(QR에 담긴 값)로 상품 상세(`/buyer/lockers/$number`)로
 * 이동하는 흐름을 데모로 붙였다(실제 QR 디코딩은 아직 없어 16번으로 고정).
 */
function ScanPage() {
  const router = useRouter();
  const [showReservedModal, setShowReservedModal] = useState(true);

  return (
    <PageContainer className="bg-[#1a1a1a]">
      <div className="flex h-[54px] items-center px-4">
        <Link
          to="/"
          aria-label="닫기"
          className="flex size-6 items-center justify-center text-white"
        >
          <X className="size-6" />
        </Link>
      </div>

      <div className="mt-[70px] flex flex-col items-center gap-6 px-4 text-center">
        <div className="flex flex-col items-center gap-2">
          <h1 className="text-lg font-bold text-white">
            QR 코드를 스캔해 주세요
          </h1>
          <p className="text-[13px] text-white/70">
            사물함 앞면의 QR 코드를 사각형 안에 맞춰 주세요
          </p>
        </div>

        <button
          type="button"
          onClick={() =>
            router.navigate({
              to: "/buyer/lockers/$number",
              params: { number: "16" },
            })
          }
          className="relative size-[240px] rounded-[20px] border border-white/35"
          aria-label="스캔 (데모: 상품 상세로 이동)"
        >
          {SCAN_FRAME_CORNERS.map((position, index) => (
            <span
              key={index}
              className={`absolute rounded-[2px] bg-[var(--color-primary)] ${position}`}
            />
          ))}
          <span className="absolute top-1/2 left-[10px] right-[10px] h-0.5 -translate-y-1/2 bg-[var(--color-primary)]" />
        </button>

        <p className="text-[13px] text-white/60">16번 사물함</p>
      </div>

      <Dialog open={showReservedModal} onOpenChange={setShowReservedModal}>
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            이미 예약된 사물함이에요
          </DialogTitle>
          <ul className="flex flex-col gap-2 text-[13px] text-[var(--color-text-muted)]">
            <li>· 다른 사용자가 16번 사물함을 예약했어요.</li>
            <li>· 판매 등록이 끝나면 상품 정보를 확인할 수 있어요.</li>
            <li>· 예약 후 3시간 안에 등록되지 않으면 자동으로 취소돼요.</li>
          </ul>
          <Button
            fullWidth
            size="lg"
            onClick={() => setShowReservedModal(false)}
          >
            확인
          </Button>
        </DialogContent>
      </Dialog>
    </PageContainer>
  );
}
