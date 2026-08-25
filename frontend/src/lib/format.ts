/** 가격을 "300,000원" 형태로 표시한다. */
export function formatPrice(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"] as const;

/** ISO 날짜 문자열을 "8/25(월) 15:10" 형태로 표시한다. */
export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  const weekday = WEEKDAYS[date.getDay()];
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  return `${date.getMonth() + 1}/${date.getDate()}(${weekday}) ${hh}:${mm}`;
}

/** ISO 날짜 문자열을 "8/24" 형태로 표시한다. */
export function formatShortDate(iso: string): string {
  const date = new Date(iso);
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

/** 만료 시각까지 남은 시간을 "6일 남음" / "3시간 남음" / "곧 만료" 형태로 표시한다. */
export function formatRemaining(
  expiresAtIso: string,
  now: Date = new Date(),
): string {
  const diffMs = new Date(expiresAtIso).getTime() - now.getTime();
  if (diffMs <= 0) return "곧 만료";

  const diffMinutes = Math.floor(diffMs / (60 * 1000));
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffDays >= 1) return `${diffDays}일 남음`;
  if (diffHours >= 1) return `${diffHours}시간 남음`;
  return `${Math.max(diffMinutes, 1)}분 남음`;
}

/**
 * 만료 시각까지 남은 시간을 "1:23"(시:분) 형태로 표시한다.
 * 사물함 현황 그리드의 예약중 셀처럼 좁은 공간에 쓴다 (Figma "01 홈 · 사물함 현황").
 */
export function formatCountdown(
  expiresAtIso: string,
  now: Date = new Date(),
): string {
  const diffMs = new Date(expiresAtIso).getTime() - now.getTime();
  if (diffMs <= 0) return "0:00";

  const totalMinutes = Math.floor(diffMs / (60 * 1000));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${hours}:${String(minutes).padStart(2, "0")}`;
}

/**
 * 만료 시각까지 남은 날짜를 "D-3" / 오늘 만료면 "D-Day" 형태로 표시한다.
 * 사물함 현황 그리드의 판매중 셀에 쓴다 (Figma "01 홈 · 사물함 현황").
 */
export function formatDday(
  expiresAtIso: string,
  now: Date = new Date(),
): string {
  const diffMs = new Date(expiresAtIso).getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (24 * 60 * 60 * 1000));
  return diffDays <= 0 ? "D-Day" : `D-${diffDays}`;
}
