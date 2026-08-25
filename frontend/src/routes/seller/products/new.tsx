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
import { useUploadImage } from "@/api/generated/images/images";
import { useRegisterProduct } from "@/api/generated/products/products";

export const Route = createFileRoute("/seller/products/new")({
  component: NewProductPage,
});

const MAX_PHOTOS = 10;

/**
 * 상품 등록 (Figma "06 상품 등록"). 사진은 첫 번째 파일만 업로드해
 * `POST /images`로 URL을 받은 뒤, 그 URL을 `POST /products`의
 * `imageUrl`로 그대로 넣는다 (여러 장 첨부는 아직 백엔드가 1장만 지원).
 */
function NewProductPage() {
  const router = useRouter();
  const [photos, setPhotos] = useState<File[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");

  const uploadImage = useUploadImage();
  const registerProduct = useRegisterProduct();
  const isSubmitting = uploadImage.isPending || registerProduct.isPending;

  const canSubmit = name.trim().length > 0 && price.trim().length > 0;

  const handleSubmit = async () => {
    if (!canSubmit || isSubmitting) return;

    try {
      let imageUrl: string | undefined;
      if (photos[0]) {
        const uploaded = await uploadImage.mutateAsync({
          data: { file: photos[0] },
        });
        imageUrl = uploaded.imageUrl;
      }

      await registerProduct.mutateAsync({
        data: {
          name: name.trim(),
          price: Number(price),
          description: description.trim() || undefined,
          imageUrl,
        },
      });

      toast.success("상품이 등록됐어요.");
      router.history.back();
    } catch {
      toast.error("상품 등록에 실패했어요.");
    }
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
                {photos.length} / {MAX_PHOTOS}
              </span>
              <input
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={(event) =>
                  setPhotos((current) =>
                    [...current, ...Array.from(event.target.files ?? [])].slice(
                      0,
                      MAX_PHOTOS,
                    ),
                  )
                }
              />
            </label>
          </div>
          {photos.length > 1 && (
            <p className="text-xs text-[var(--color-text-muted)]">
              지금은 첫 번째 사진만 등록에 사용돼요.
            </p>
          )}
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
            value={description}
            onChange={(event) => setDescription(event.target.value)}
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
          disabled={!canSubmit || isSubmitting}
          onClick={handleSubmit}
        >
          작성 완료
        </Button>
      </div>
    </PageContainer>
  );
}
