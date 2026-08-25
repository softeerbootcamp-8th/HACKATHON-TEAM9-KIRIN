import { useEffect, useRef, useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { Camera, X } from "lucide-react";
import { toast } from "sonner";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { useUploadImage } from "@/api/generated/images/images";
import {
  useRegisterProduct,
  getGetMyProductsQueryKey,
} from "@/api/generated/products/products";

export const Route = createFileRoute("/seller/products/new")({
  component: NewProductPage,
});

const MAX_PHOTOS = 10;
const NAME_MAX_LENGTH = 32;
const DESCRIPTION_MAX_LENGTH = 500;
const MIN_PRICE = 1_000;
const MAX_PRICE = 1_000_000_000;

type Photo = { id: string; url: string; file: File };

/**
 * 상품 등록 (Figma "06 상품 등록"). 사진은 여러 장 미리보기로 보여주지만
 * 첫 번째 사진만 업로드해 `POST /images`로 URL을 받은 뒤, 그 URL을
 * `POST /products`의 `imageUrl`로 그대로 넣는다 (여러 장 첨부는 아직
 * 백엔드가 1장만 지원).
 */
function NewProductPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [photos, setPhotos] = useState<Photo[]>([]);
  const photosRef = useRef<Photo[]>([]);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");

  const uploadImage = useUploadImage();
  const registerProduct = useRegisterProduct();
  const isSubmitting = uploadImage.isPending || registerProduct.isPending;

  const canSubmit =
    photos.length > 0 && name.trim().length > 0 && price.trim().length > 0;

  useEffect(() => {
    photosRef.current = photos;
  }, [photos]);

  // 언마운트 시 미리보기용 objectURL 을 정리한다.
  useEffect(() => {
    return () => {
      photosRef.current.forEach((photo) => URL.revokeObjectURL(photo.url));
    };
  }, []);

  const handleFilesSelected = (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const remaining = MAX_PHOTOS - photos.length;
    const newPhotos = Array.from(files)
      .slice(0, remaining)
      .map((file) => ({
        id: `${file.name}-${file.lastModified}-${Math.random().toString(36).slice(2)}`,
        url: URL.createObjectURL(file),
        file,
      }));
    setPhotos((prev) => [...prev, ...newPhotos]);
  };

  const removePhoto = (id: string) => {
    setPhotos((prev) => {
      const target = prev.find((photo) => photo.id === id);
      if (target) URL.revokeObjectURL(target.url);
      return prev.filter((photo) => photo.id !== id);
    });
  };

  const handleSubmit = async () => {
    if (!canSubmit || isSubmitting) return;

    const priceValue = Number(price);
    if (priceValue < MIN_PRICE || priceValue > MAX_PRICE) {
      toast.error("판매 가격은 최소 1천원, 최대 10억원까지 설정할 수 있어요.");
      return;
    }

    try {
      let imageUrl: string | undefined;
      if (photos[0]) {
        const uploaded = await uploadImage.mutateAsync({
          data: { file: photos[0].file },
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

      // 뒤로 돌아갈 화면(예약 상품 선택, 내 리스트)이 캐시된 목록을 그대로
      // 보여주지 않도록 방금 등록한 상품이 반영되게 무효화한다.
      queryClient.invalidateQueries({ queryKey: getGetMyProductsQueryKey({}) });

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
          <div className="flex flex-wrap gap-2">
            {photos.map((photo) => (
              <div
                key={photo.id}
                className="relative size-[88px] shrink-0 overflow-hidden rounded-[var(--radius-sm)] border border-[var(--color-border)]"
              >
                <img
                  src={photo.url}
                  alt=""
                  className="size-full object-cover"
                />
                <button
                  type="button"
                  onClick={() => removePhoto(photo.id)}
                  aria-label="사진 삭제"
                  className="absolute top-1 right-1 flex size-5 items-center justify-center rounded-full bg-black/60 text-white"
                >
                  <X className="size-3" />
                </button>
              </div>
            ))}

            {photos.length < MAX_PHOTOS && (
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
                  onChange={(event) => {
                    handleFilesSelected(event.target.files);
                    event.target.value = "";
                  }}
                />
              </label>
            )}
          </div>
          {photos.length === 0 ? (
            <p className="text-xs text-[var(--color-danger)]">
              사진을 1장 이상 등록해야 등록할 수 있어요.
            </p>
          ) : (
            photos.length > 1 && (
              <p className="text-xs text-[var(--color-text-muted)]">
                지금은 첫 번째 사진만 등록에 사용돼요.
              </p>
            )
          )}
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="product-name">상품명</Label>
            <span className="text-xs text-[var(--color-text-muted)]">
              {name.length}/{NAME_MAX_LENGTH}
            </span>
          </div>
          <Input
            id="product-name"
            placeholder="예) 캘러웨이 드라이버 10.5도"
            maxLength={NAME_MAX_LENGTH}
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="product-description">상품 설명</Label>
            <span className="text-xs text-[var(--color-text-muted)]">
              {description.length}/{DESCRIPTION_MAX_LENGTH}
            </span>
          </div>
          <Textarea
            id="product-description"
            className="h-[120px]"
            placeholder="상품 상태, 구입 시기, 사용감 등을 적어주세요."
            maxLength={DESCRIPTION_MAX_LENGTH}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="product-price">판매 가격</Label>
          <div className="flex items-center rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-bg)] p-3.5">
            <input
              id="product-price"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              placeholder="0"
              value={price}
              onChange={(event) =>
                setPrice(event.target.value.replace(/[^0-9]/g, ""))
              }
              className="w-full min-w-0 bg-transparent text-sm text-foreground outline-none placeholder:text-[var(--color-text-placeholder)]"
            />
            <span className="text-sm font-medium text-[var(--color-text-muted)]">
              원
            </span>
          </div>
          <p className="text-xs text-[var(--color-text-placeholder)]">
            최소 1천원 ~ 최대 10억원
          </p>
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
