import { useRef, useState } from "react";
import { cn } from "@/lib/utils";

type PhotoCarouselProps = {
  imageUrls: string[];
  alt: string;
  className?: string;
};

/**
 * 상품 사진 여러 장을 좌우로 넘겨보는 캐러셀 (구매자 상품 상세 전용).
 * 별도 라이브러리 없이 네이티브 스크롤 스냅으로 구현한다 — 스와이프는
 * 브라우저 스크롤이 그대로 처리하고, 지금 몇 번째 사진인지는 스크롤
 * 위치로 계산해 하단 점 인디케이터에 반영한다.
 */
export function PhotoCarousel({ imageUrls, alt, className }: PhotoCarouselProps) {
  const [activeIndex, setActiveIndex] = useState(0);
  const trackRef = useRef<HTMLDivElement>(null);

  if (imageUrls.length === 0) {
    return (
      <div
        className={cn(
          "flex h-[220px] w-full items-center justify-center bg-[var(--color-surface-2)] text-sm text-[var(--color-text-muted)]",
          className,
        )}
      >
        상품 이미지
      </div>
    );
  }

  const handleScroll = () => {
    const track = trackRef.current;
    if (!track || track.clientWidth === 0) return;
    const index = Math.round(track.scrollLeft / track.clientWidth);
    setActiveIndex(Math.min(index, imageUrls.length - 1));
  };

  const scrollToIndex = (index: number) => {
    const track = trackRef.current;
    if (!track) return;
    track.scrollTo({ left: index * track.clientWidth, behavior: "smooth" });
  };

  return (
    <div className={cn("relative h-[220px] w-full", className)}>
      <div
        ref={trackRef}
        onScroll={handleScroll}
        className="flex h-full w-full snap-x snap-mandatory overflow-x-auto scroll-smooth [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {imageUrls.map((url, index) => (
          <img
            key={url + index}
            src={url}
            alt={imageUrls.length > 1 ? `${alt} ${index + 1}/${imageUrls.length}` : alt}
            className="h-full w-full shrink-0 snap-center object-cover"
          />
        ))}
      </div>

      {imageUrls.length > 1 && (
        <div className="absolute bottom-2.5 left-1/2 flex -translate-x-1/2 gap-1.5">
          {imageUrls.map((url, index) => (
            <button
              key={url + index}
              type="button"
              aria-label={`${index + 1}번째 사진 보기`}
              aria-current={index === activeIndex}
              onClick={() => scrollToIndex(index)}
              className={cn(
                "size-1.5 rounded-full transition-colors",
                index === activeIndex
                  ? "bg-[var(--color-bg)]"
                  : "bg-[var(--color-bg)]/50",
              )}
            />
          ))}
        </div>
      )}
    </div>
  );
}
