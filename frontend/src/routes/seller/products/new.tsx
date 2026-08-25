import { useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { Camera } from "lucide-react";
import { toast } from "sonner";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/seller/products/new")({
  component: NewProductPage,
});

const MAX_PHOTOS = 10;

/**
 * 상품 등록 (Figma "06 상품 등록").
 * 사진 업로드는 아직 백엔드 연동 전이라 개수만 표시한다 — 실제 업로드는
 * 이미지 스토리지 API가 정해지면 붙인다.
 */
function NewProductPage() {
  const router = useRouter();
  const [photoCount, setPhotoCount] = useState(0);
  const [name, setName] = useState("");
  const [price, setPrice] = useState("");

  const canSubmit = name.trim().length > 0 && price.trim().length > 0;

  const handleSubmit = () => {
    toast.success("상품이 등록됐어요.");
    router.history.back();
  };

  return (
    <PageContainer>
      <Header title="상품 등록" onBack={() => router.history.back()} />

      <div className="flex flex-col gap-[18px] px-4 pt-2">
        <div className="flex flex-col gap-2">
          <Label>상품 사진</Label>
          <div className="flex gap-2">
            <label className="flex size-[88px] shrink-0 flex-col items-center justify-center gap-1 rounded-[var(--radius-sm)] border border-dashed border-[var(--color-border)] bg-[var(--color-surface-2)] text-[var(--color-text-muted)]">
              <Camera className="size-5" />
              <span className="text-[11px]">
                {photoCount} / {MAX_PHOTOS}
              </span>
              <input
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={(event) =>
                  setPhotoCount((count) =>
                    Math.min(
                      MAX_PHOTOS,
                      count + (event.target.files?.length ?? 0),
                    ),
                  )
                }
              />
            </label>
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="product-name">상품명</Label>
          <Input
            id="product-name"
            placeholder="예) 캘러웨이 드라이버 10.5도"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="product-description">상품 설명</Label>
          <Textarea
            id="product-description"
            className="h-[120px]"
            placeholder="상품 상태, 구입 시기, 사용감 등을 적어주세요."
          />
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="product-price">판매 가격</Label>
          <div className="flex items-center rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-bg)] p-3.5">
            <input
              id="product-price"
              type="number"
              min={0}
              placeholder="0"
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              className="w-full min-w-0 bg-transparent text-sm text-foreground outline-none placeholder:text-[var(--color-text-placeholder)]"
            />
            <span className="text-sm font-medium text-[var(--color-text-muted)]">
              원
            </span>
          </div>
        </div>
      </div>

      <div className="mt-auto flex flex-col border-t border-[var(--color-border)] px-4 py-3">
        <Button
          fullWidth
          size="lg"
          disabled={!canSubmit}
          onClick={handleSubmit}
        >
          작성 완료
        </Button>
      </div>
    </PageContainer>
  );
}
