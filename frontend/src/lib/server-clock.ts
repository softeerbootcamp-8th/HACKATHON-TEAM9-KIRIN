/**
 * 클라이언트 기기 시계가 서버와 몇 초씩 어긋나 있으면(NTP 미동기화 등) 예약·판매
 * 남은시간 타이머가 실제와 다르게 보인다. HTTP 응답에는 항상 `Date` 헤더(서버 시각,
 * 초 단위)가 붙어 오므로, 매 API 응답마다 이를 클라이언트 시계와 비교해 오차를
 * 추정해 두고 "지금"이 필요한 곳에서는 그 오차를 보정한 시각을 쓴다.
 * (api/mutator/custom-instance.ts의 응답 인터셉터가 매 요청마다 갱신한다.)
 */
let clockOffsetMs = 0;

export function updateServerClockOffset(serverDateHeader: string): void {
  const serverTime = new Date(serverDateHeader).getTime();
  if (Number.isNaN(serverTime)) return;
  clockOffsetMs = serverTime - Date.now();
}

/** 서버 시계 기준으로 보정한 현재 시각. */
export function serverNow(): Date {
  return new Date(Date.now() + clockOffsetMs);
}
