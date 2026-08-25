import { useEffect, useRef, useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { Camera, X } from "lucide-react";
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
const NAME_MAX_LENGTH = 32;
const DESCRIPTION_MAX_LENGTH = 500;

type Photo = { id: string; url: string };

/**
 * 상품 등록 (Figma "06 상품 등록").
 * 사진은 아직 백엔드 연동 전이라 브라우저 안에서만 미리보기(objectURL)로 보여준다
 * — 실제 업로드는 이미지 스토리지 API가 정해지면 붙인다.
 */
function NewProductPage() {
  const router = useRouter();
  const [photos, setPhotos] = useState<Photo[]>([]);
  const photosRef = useRef<Photo[]>([]);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");

  const canSubmit = name.trim().length > 0 && price.trim().length > 0;

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
