import { useEffect, useRef, useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import axios from "axios";
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
  useUpdateProduct,
  useGetProduct,
  getGetMyProductsQueryKey,
  getGetProductQueryKey,
} from "@/api/generated/products/products";
import type { ErrorResponse } from "@/api/generated/model";

type NewProductSearch = { productId?: number };

export const Route = createFileRoute("/seller/products/new")({
  validateSearch: (search: Record<string, unknown>): NewProductSearch => ({
    productId: search.productId != null ? Number(search.productId) : undefined,
  }),
  component: NewProductPage,
});

const MAX_PHOTOS = 10;
const NAME_MAX_LENGTH = 32;
const DESCRIPTION_MAX_LENGTH = 500;
const MIN_PRICE = 1_000;
const MAX_PRICE = 1_000_000_000;

/** 새로 고른 사진은 file을 들고 있고, 수정 화면에 프리필된 기존 사진은 이미
 * 업로드돼 있어 file 없이 서버 URL만 가진다. */
type Photo = { id: string; url: string; file?: File };

const HEIC_CONTENT_TYPES = new Set(["image/heic", "image/heif"]);

/** 아이폰 사진 라이브러리는 기본적으로 HEIC/HEIF로 저장하는데, 백엔드가
 * 지원하지 않는 형식이라(jpeg/png/webp/gif만 허용) 그대로 올리면 등록이
 * 실패한다. iOS는 file input이 content-type을 빈 문자열로 주는 경우도 있어
 * 확장자도 같이 본다. */
function isHeicFile(file: File) {
  return (
    HEIC_CONTENT_TYPES.has(file.type.toLowerCase()) ||
    /\.(heic|heif)$/i.test(file.name)
  );
}

/** HEIC/HEIF 사진을 업로드 가능한 JPEG로 변환한다. 변환 라이브러리(heic2any)는
 * 용량이 커서 HEIC 파일을 실제로 선택했을 때만 동적으로 불러온다. */
async function toUploadableFile(file: File): Promise<File> {
  if (!isHeicFile(file)) return file;

  const { default: heic2any } = await import("heic2any");
  const converted = await heic2any({
    blob: file,
    toType: "image/jpeg",
    quality: 0.85,
  });
  const blob = Array.isArray(converted) ? converted[0] : converted;
  const jpegName = file.name.replace(/\.(heic|heif)$/i, "") + ".jpg";
  return new File([blob], jpegName, { type: "image/jpeg" });
}

/** 서버가 내려준 에러 메시지가 있으면 그대로 보여주고, 없으면 기본 문구를 쓴다. */
function extractErrorMessage(error: unknown, fallback: string) {
  if (
    axios.isAxiosError<ErrorResponse>(error) &&
    error.response?.data.message
  ) {
    return error.response.data.message;
  }
  return fallback;
}

/**
 * 상품 등록 · 수정 (Figma "06 상품 등록"). 등록 시엔 고른 사진을 전부
 * `POST /images`로 순서대로 업로드해 URL 배열을 만든 뒤, `POST /products`의
 * `imageUrls`로 그대로 넣는다. 구매자 상세 화면에서는 이 순서 그대로
 * 캐러셀로 보여준다.
 *
 * `?productId=`가 있으면 같은 화면을 수정 모드로 재활용한다 — 기존 상품
 * 정보를 불러와 값을 채워 넣고, 제출 시 등록 대신 `PATCH /products/{id}`를
 * 호출한다. 사진을 새로 고르지 않으면 기존 URL을 그대로 재사용해 다시
 * 업로드하지 않는다.
 */
function NewProductPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { productId } = Route.useSearch();
  const isEditMode = productId != null;

  const { data: existingProduct } = useGetProduct(productId ?? 0, {
    query: { enabled: isEditMode },
  });

  const [photos, setPhotos] = useState<Photo[]>([]);
  const photosRef = useRef<Photo[]>([]);
  const prefilledRef = useRef(false);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [isConvertingPhotos, setIsConvertingPhotos] = useState(false);

  const uploadImage = useUploadImage();
  const registerProduct = useRegisterProduct();
  const updateProduct = useUpdateProduct();
  const isSubmitting =
    uploadImage.isPending ||
    registerProduct.isPending ||
    updateProduct.isPending;

  const canSubmit =
    photos.length > 0 && name.trim().length > 0 && price.trim().length > 0;

  useEffect(() => {
    photosRef.current = photos;
  }, [photos]);

  // 수정 모드로 기존 상품이 로드되면 값을 한 번만 채워 넣는다 — 백그라운드
  // 리페치가 다시 와도 사용자가 입력 중인 값을 덮어쓰지 않도록 한다.
  useEffect(() => {
    if (!existingProduct || prefilledRef.current) return;
    prefilledRef.current = true;
    setName(existingProduct.name);
    setDescription(existingProduct.description ?? "");
    setPrice(String(existingProduct.price));
    setPhotos(
      existingProduct.imageUrls.map((url, index) => ({
        id: `existing-${index}-${url}`,
        url,
      })),
    );
  }, [existingProduct]);

  // 언마운트 시 새로 고른 사진의 미리보기용 objectURL 만 정리한다 —
  // 기존 사진의 url은 실제 서버 URL이라 회수 대상이 아니다.
  useEffect(() => {
    return () => {
      photosRef.current.forEach((photo) => {
        if (photo.file) URL.revokeObjectURL(photo.url);
      });
    };
  }, []);

  const handleFilesSelected = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const remaining = MAX_PHOTOS - photos.length;
    const selectedFiles = Array.from(files).slice(0, remaining);

    setIsConvertingPhotos(true);
    try {
      const uploadableFiles = await Promise.all(
        selectedFiles.map(toUploadableFile),
      );
      const newPhotos = uploadableFiles.map((file) => ({
        id: `${file.name}-${file.lastModified}-${Math.random().toString(36).slice(2)}`,
        url: URL.createObjectURL(file),
        file,
      }));
      setPhotos((prev) => [...prev, ...newPhotos]);
    } catch {
      toast.error("사진을 처리하지 못했어요. 다른 사진을 선택해 주세요.");
    } finally {
      setIsConvertingPhotos(false);
    }
  };

  const removePhoto = (id: string) => {
    setPhotos((prev) => {
      const target = prev.find((photo) => photo.id === id);
      if (target?.file) URL.revokeObjectURL(target.url);
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
      // 기존 사진(file 없음)은 이미 업로드돼 있으니 URL을 그대로 쓰고,
      // 새로 고른 사진(file 있음)만 업로드한다.
      const imageUrls = await Promise.all(
        photos.map((photo) =>
          photo.file
            ? uploadImage
                .mutateAsync({ data: { file: photo.file } })
                .then((result) => result.imageUrl)
            : Promise.resolve(photo.url),
        ),
      );

      const data = {
        name: name.trim(),
        price: Number(price),
        description: description.trim() || undefined,
        imageUrls,
      };

      if (isEditMode) {
        await updateProduct.mutateAsync({ productId, data });
        queryClient.invalidateQueries({
          queryKey: getGetProductQueryKey(productId),
        });
        queryClient.invalidateQueries({
          queryKey: getGetMyProductsQueryKey({}),
        });
        toast.success("상품 정보가 수정됐어요.");
      } else {
        await registerProduct.mutateAsync({ data });
        // 뒤로 돌아갈 화면(예약 상품 선택, 내 리스트)이 캐시된 목록을 그대로
        // 보여주지 않도록 방금 등록한 상품이 반영되게 무효화한다.
        queryClient.invalidateQueries({
          queryKey: getGetMyProductsQueryKey({}),
        });
        toast.success("상품이 등록됐어요.");
      }

      router.history.back();
    } catch (error) {
      toast.error(
        extractErrorMessage(
          error,
          isEditMode ? "상품 수정에 실패했어요." : "상품 등록에 실패했어요.",
        ),
      );
    }
  };

  return (
    <PageContainer>
      <Header
        title={isEditMode ? "상품 수정" : "상품 등록"}
        onBack={() => router.history.back()}
      />

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
              <label
                className={`flex size-[88px] shrink-0 flex-col items-center justify-center gap-1 rounded-[var(--radius-sm)] border border-dashed border-[var(--color-border)] bg-[var(--color-surface-2)] text-[var(--color-text-muted)] ${isConvertingPhotos ? "opacity-50" : ""}`}
              >
                <Camera className="size-5" />
                <span className="text-[11px]">
                  {isConvertingPhotos
                    ? "처리 중..."
                    : `${photos.length} / ${MAX_PHOTOS}`}
                </span>
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  className="hidden"
                  disabled={isConvertingPhotos}
                  onChange={(event) => {
                    void handleFilesSelected(event.target.files);
                    event.target.value = "";
                  }}
                />
              </label>
            )}
          </div>
          {photos.length === 0 && (
            <p className="text-xs text-[var(--color-danger)]">
              사진을 1장 이상 첨부해야 등록할 수 있어요.
            </p>
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
          disabled={!canSubmit || isSubmitting || isConvertingPhotos}
          onClick={handleSubmit}
        >
          {isEditMode ? "수정 완료" : "작성 완료"}
        </Button>
      </div>
    </PageContainer>
  );
}
