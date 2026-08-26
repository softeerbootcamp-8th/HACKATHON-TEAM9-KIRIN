import { beforeEach, describe, expect, test, vi } from "vitest";
import { serverNow, updateServerClockOffset } from "./server-clock";

describe("serverNow", () => {
  beforeEach(() => {
    updateServerClockOffset(new Date(Date.now()).toUTCString());
  });

  test("서버 시각을 반영한 오차가 없으면 클라이언트 시각과 거의 같다", () => {
    // given: Date 헤더(toUTCString)는 초 단위라 최대 1초 미만의 절삭 오차가 남을 수 있다.
    const before = Date.now();
    const result = serverNow().getTime();

    expect(Math.abs(result - before)).toBeLessThan(1000);
  });

  test("서버가 클라이언트보다 앞서 있으면 그 오차만큼 보정한다", () => {
    const clientNow = Date.now();
    const serverAheadMs = 5000;
    updateServerClockOffset(new Date(clientNow + serverAheadMs).toUTCString());

    const result = serverNow().getTime();

    expect(result).toBeGreaterThanOrEqual(clientNow + serverAheadMs - 1000);
  });

  test("Date 헤더를 파싱할 수 없으면 오차를 갱신하지 않고 이전 값을 유지한다", () => {
    updateServerClockOffset(new Date(Date.now() + 10000).toUTCString());
    const before = serverNow().getTime();

    updateServerClockOffset("not-a-date");
    const after = serverNow().getTime();

    expect(after).toBeGreaterThanOrEqual(before - 100);
  });
});

describe("updateServerClockOffset", () => {
  test("실제 HTTP Date 헤더 형식을 파싱한다", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-08-26T12:00:00.000Z"));

    updateServerClockOffset("Wed, 26 Aug 2026 12:00:03 GMT");
    const result = serverNow().getTime();

    expect(result).toBe(new Date("2026-08-26T12:00:03.000Z").getTime());
    vi.useRealTimers();
  });
});
