import { describe, expect, test } from "vitest";
import { formatDday } from "./format";

describe("formatDday", () => {
  test("판매 시작 직후처럼 남은 시간이 정확히 7일에서 살짝 넘치면 D-7로 표시한다", () => {
    // given: "바로 팔기" 직후처럼 클라이언트 시계가 서버보다 500ms 앞선 상황을 흉내낸다.
    const now = new Date("2026-08-25T12:00:00.000Z");
    const expiresAt = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000 + 500);

    // when
    const result = formatDday(expiresAt.toISOString(), now);

    // then
    expect(result).toBe("D-7");
  });

  test("정확히 7일 남았으면 D-7로 표시한다", () => {
    // given
    const now = new Date("2026-08-25T12:00:00.000Z");
    const expiresAt = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);

    // when
    const result = formatDday(expiresAt.toISOString(), now);

    // then
    expect(result).toBe("D-7");
  });

  test("여유 시간(1분)을 넘겨 다음날로 접어들면 D-8로 표시한다", () => {
    // given
    const now = new Date("2026-08-25T12:00:00.000Z");
    const expiresAt = new Date(
      now.getTime() + 7 * 24 * 60 * 60 * 1000 + 2 * 60 * 1000,
    );

    // when
    const result = formatDday(expiresAt.toISOString(), now);

    // then
    expect(result).toBe("D-8");
  });

  test("이미 만료됐으면 D-Day로 표시한다", () => {
    // given
    const now = new Date("2026-08-25T12:00:00.000Z");
    const expiresAt = new Date(now.getTime() - 1000);

    // when
    const result = formatDday(expiresAt.toISOString(), now);

    // then
    expect(result).toBe("D-Day");
  });
});
