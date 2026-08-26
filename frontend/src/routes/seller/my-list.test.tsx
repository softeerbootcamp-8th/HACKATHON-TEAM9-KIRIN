import { useRef } from "react";
import { describe, expect, test, vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { ProductMoreMenu } from "./my-list";
import type { ProductSummaryResponse } from "@/api/generated/model";

// jsdom은 PointerEvent가 없어 MouseEvent로 대신 만든다 — Radix 드롭다운은
// pointerdown으로 열리기 때문에 이게 없으면 테스트에서 메뉴 자체가 안 열린다.
if (typeof window.PointerEvent === "undefined") {
  class PointerEventPolyfill extends MouseEvent {}
  // @ts-expect-error - jsdom 테스트 환경 전용 폴리필
  window.PointerEvent = PointerEventPolyfill;
}

function 상품(
  overrides: Partial<ProductSummaryResponse> = {},
): ProductSummaryResponse {
  return {
    productId: 1,
    lockerId: null,
    name: "무선 이어폰",
    price: 45000,
    imageUrl: null,
    status: "PREPARING",
    reservationExpiresAt: null,
    sellingStartedAt: null,
    sellingExpiresAt: null,
    soldAt: null,
    ...overrides,
  };
}

/** 더보기 버튼을 눌러 드롭다운을 연다. Radix는 pointerdown에서 메뉴를 연다. */
function 더보기_열기() {
  fireEvent.pointerDown(screen.getByRole("button", { name: "더보기" }), {
    button: 0,
    ctrlKey: false,
  });
}

describe("ProductMoreMenu", () => {
  test("판매대기중_상품에서_상품_수정을_누르면_onEdit이_호출된다", () => {
    // given
    const product = 상품({ status: "PREPARING" });
    const onEdit = vi.fn();
    render(
      <ProductMoreMenu
        product={product}
        onEdit={onEdit}
        onStartDeposit={vi.fn()}
        onRequestCancelReservation={vi.fn()}
        onEndSelling={vi.fn()}
        onRequestDelete={vi.fn()}
      />,
    );

    // when
    더보기_열기();
    fireEvent.click(screen.getByText("상품 수정"));

    // then
    expect(onEdit).toHaveBeenCalledWith(product);
  });

  test("판매대기중_상품에서_삭제를_누르면_onRequestDelete가_호출된다", () => {
    // given
    const product = 상품({ status: "PREPARING" });
    const onRequestDelete = vi.fn();
    render(
      <ProductMoreMenu
        product={product}
        onEdit={vi.fn()}
        onStartDeposit={vi.fn()}
        onRequestCancelReservation={vi.fn()}
        onEndSelling={vi.fn()}
        onRequestDelete={onRequestDelete}
      />,
    );

    // when
    더보기_열기();
    fireEvent.click(screen.getByText("삭제"));

    // then
    expect(onRequestDelete).toHaveBeenCalledWith(product);
  });

  test("예약중_상품은_물건_넣기와_예약_취소_항목을_보여준다", () => {
    // given
    const product = 상품({
      status: "RESERVED",
      lockerId: 13,
      reservationExpiresAt: "2026-08-25T16:00:00",
    });
    const onStartDeposit = vi.fn();
    const onRequestCancelReservation = vi.fn();
    render(
      <ProductMoreMenu
        product={product}
        onEdit={vi.fn()}
        onStartDeposit={onStartDeposit}
        onRequestCancelReservation={onRequestCancelReservation}
        onEndSelling={vi.fn()}
        onRequestDelete={vi.fn()}
      />,
    );

    // when
    더보기_열기();
    fireEvent.click(screen.getByText("물건 넣기"));

    // then
    expect(onStartDeposit).toHaveBeenCalledWith(product);
  });

  test("판매중_상품에서_판매_중단을_누르면_onEndSelling이_호출된다", () => {
    // given
    const product = 상품({ status: "SELLING", lockerId: 16 });
    const onEndSelling = vi.fn();
    render(
      <ProductMoreMenu
        product={product}
        onEdit={vi.fn()}
        onStartDeposit={vi.fn()}
        onRequestCancelReservation={vi.fn()}
        onEndSelling={onEndSelling}
        onRequestDelete={vi.fn()}
      />,
    );

    // when
    더보기_열기();
    fireEvent.click(screen.getByText("판매 중단"));

    // then
    expect(onEndSelling).toHaveBeenCalledWith(product);
  });

  test("카드를_감싸는_링크의_클릭으로_새지_않는다", () => {
    // given: 실제 my-list.tsx처럼 카드 전체가 상세 화면 이동용 <a>로 감싸져 있다.
    // DropdownMenuContent는 Portal로 document.body에 그려지지만, React는 포탈
    // 내부 이벤트를 DOM 위치가 아니라 React 트리 기준으로 버블시키기 때문에
    // 막지 않으면 메뉴 클릭이 감싸는 <a>의 onClick까지 같이 실행된다.
    const product = 상품({ status: "PREPARING" });
    const onAnchorClick = vi.fn();
    const onEdit = vi.fn();
    render(
      <a href="/seller/products/1" onClick={onAnchorClick}>
        <ProductMoreMenu
          product={product}
          onEdit={onEdit}
          onStartDeposit={vi.fn()}
          onRequestCancelReservation={vi.fn()}
          onEndSelling={vi.fn()}
          onRequestDelete={vi.fn()}
        />
      </a>,
    );

    // when
    더보기_열기();
    fireEvent.click(screen.getByText("상품 수정"));

    // then
    expect(onEdit).toHaveBeenCalledWith(product);
    expect(onAnchorClick).not.toHaveBeenCalled();
  });

  test("드롭다운이_열린_상태에서_다른_곳을_누르면_닫힌다", async () => {
    // given: Radix DropdownMenu는 "바깥 클릭 감지" 리스너를 마운트 다음 매크로태스크에
    // 등록한다(같은 클릭으로 열자마자 바로 닫히는 걸 막기 위해서). 그래서 이 테스트도
    // 실제 사용자처럼 열고 나서 한 틱 기다린 뒤에 바깥을 눌러야 한다.
    const product = 상품({ status: "PREPARING" });
    render(
      <div>
        <div data-testid="outside">다른 영역</div>
        <ProductMoreMenu
          product={product}
          onEdit={vi.fn()}
          onStartDeposit={vi.fn()}
          onRequestCancelReservation={vi.fn()}
          onEndSelling={vi.fn()}
          onRequestDelete={vi.fn()}
        />
      </div>,
    );

    // when
    더보기_열기();
    expect(screen.getByText("상품 수정")).toBeInTheDocument();

    await act(() => new Promise((resolve) => setTimeout(resolve, 0)));
    fireEvent.pointerDown(screen.getByTestId("outside"));

    // then
    expect(screen.queryByText("상품 수정")).not.toBeInTheDocument();
  });

  test("드롭다운이_열린_상태에서_카드의_다른_부분을_누르면_상세_화면으로_이동하지_않는다", async () => {
    // given: my-list.tsx와 동일한 구성 — 카드 전체를 감싼 <a>가 onClick으로
    // 상세 화면 이동을 흉내 내고, ProductMoreMenu의 onPointerDownOutside가
    // "지금 막 바깥 클릭으로 닫혔다"는 걸 알려준다.
    const product = 상품({ status: "PREPARING" });
    const onAnchorClick = vi.fn();

    function TestCard() {
      const suppressCardClickRef = useRef(false);
      return (
        <a
          href={`/seller/products/${product.productId}`}
          onClick={(event) => {
            onAnchorClick();
            if (suppressCardClickRef.current) {
              suppressCardClickRef.current = false;
              event.preventDefault();
            }
          }}
        >
          <span>{product.name}</span>
          <ProductMoreMenu
            product={product}
            onEdit={vi.fn()}
            onStartDeposit={vi.fn()}
            onRequestCancelReservation={vi.fn()}
            onEndSelling={vi.fn()}
            onRequestDelete={vi.fn()}
            onPointerDownOutside={() => {
              suppressCardClickRef.current = true;
              setTimeout(() => {
                suppressCardClickRef.current = false;
              }, 0);
            }}
          />
        </a>
      );
    }
    render(<TestCard />);

    // when
    더보기_열기();
    expect(screen.getByText("상품 수정")).toBeInTheDocument();
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)));

    // 더보기 버튼도, 메뉴 항목도 아닌 카드의 다른 부분(상품명)을 누른다.
    fireEvent.pointerDown(screen.getByText(product.name));
    const notPrevented = fireEvent.click(screen.getByText(product.name));

    // then: 메뉴는 닫히고, 카드 클릭의 onClick 자체는 호출되지만
    // preventDefault로 상세 화면 이동(기본 동작)은 막혀야 한다.
    expect(screen.queryByText("상품 수정")).not.toBeInTheDocument();
    expect(onAnchorClick).toHaveBeenCalledTimes(1);
    expect(notPrevented).toBe(false);
  });
});
