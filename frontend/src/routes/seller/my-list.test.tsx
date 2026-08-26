import { useRef } from "react";
import { describe, expect, test, vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { ProductMoreMenu, sortByStatusOrder } from "./my-list";
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

describe("sortByStatusOrder", () => {
  test("등록순으로_섞여있어도_예약중_판매중_판매대기_순서로_모은다", () => {
    // given
    const products = [
      상품({ productId: 1, status: "PREPARING" }),
      상품({ productId: 2, status: "RESERVED" }),
      상품({ productId: 3, status: "SELLING" }),
      상품({ productId: 4, status: "RESERVED" }),
      상품({ productId: 5, status: "PREPARING" }),
    ];

    // when
    const result = sortByStatusOrder(products, [
      "RESERVED",
      "SELLING",
      "PREPARING",
    ]);

    // then
    expect(result.map((product) => product.productId)).toEqual([
      2, 4, 3, 1, 5,
    ]);
  });

  test("같은_상태_안에서는_원래_순서가_그대로_유지된다", () => {
    // given
    const products = [
      상품({ productId: 10, status: "RESERVED" }),
      상품({ productId: 11, status: "RESERVED" }),
      상품({ productId: 12, status: "RESERVED" }),
    ];

    // when
    const result = sortByStatusOrder(products, [
      "RESERVED",
      "SELLING",
      "PREPARING",
    ]);

    // then
    expect(result.map((product) => product.productId)).toEqual([10, 11, 12]);
  });

  test("원본_배열은_바뀌지_않는다", () => {
    // given
    const products = [
      상품({ productId: 1, status: "PREPARING" }),
      상품({ productId: 2, status: "RESERVED" }),
    ];

    // when
    sortByStatusOrder(products, ["RESERVED", "SELLING", "PREPARING"]);

    // then
    expect(products.map((product) => product.productId)).toEqual([1, 2]);
  });
});

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

  test("드롭다운이_열린_상태에서_카드의_다른_부분을_바로_눌러도_상세_화면으로_이동하지_않는다", () => {
    // given: my-list.tsx와 동일한 구성 — 카드 전체를 감싼 <a>가 pointerdown
    // 캡처 시점에 열린 드롭다운(`[role="menu"]`) DOM이 있는지를 직접 확인한다.
    // (이전엔 Radix의 onOpenChange/onPointerDownOutside 콜백에 기댔었는데, 이건
    // 실제 DOM 갱신보다 늦게 — 리액트 effect나 별도 매크로태스크에서 — 불려서
    // 빠르게 "더보기 → 다른 곳"을 연달아 누르면 놓치는 경합이 있었다. 지금
    // 방식은 렌더 커밋과 동기적으로 반영되는 DOM을 직접 보기 때문에 그 경합이 없다.)
    const product = 상품({ status: "PREPARING" });
    const onAnchorClick = vi.fn();

    function TestCard() {
      const suppressCardClickRef = useRef(false);
      return (
        <a
          href={`/seller/products/${product.productId}`}
          onPointerDownCapture={() => {
            suppressCardClickRef.current =
              document.querySelector('[role="menu"]') !== null;
          }}
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
          />
        </a>
      );
    }
    render(<TestCard />);

    // when
    더보기_열기();
    expect(screen.getByText("상품 수정")).toBeInTheDocument();

    // 더보기 버튼도, 메뉴 항목도 아닌 카드의 다른 부분(상품명)을 바로 누른다.
    fireEvent.pointerDown(screen.getByText(product.name));
    const notPrevented = fireEvent.click(screen.getByText(product.name));

    // then: 카드 클릭의 onClick 자체는 호출되지만, preventDefault로 상세
    // 화면 이동(기본 동작)은 막혀야 한다.
    expect(onAnchorClick).toHaveBeenCalledTimes(1);
    expect(notPrevented).toBe(false);
  });
});

// 참고: "act()/fireEvent 없이 raw dispatchEvent를 연달아 호출해도 막히는지"까지
// jsdom에서 검증해 보려 했으나, jsdom은 act() 밖에서는 렌더 커밋 자체(더보기
// 클릭으로 메뉴 콘텐츠가 DOM에 붙는 것)조차 동기적으로 반영하지 않는다 —
// 실제 크롬(운영 배포 사이트)에서 직접 확인한 동작과 다르다. 그래서 이 경합은
// jsdom 테스트로는 신뢰할 수 없다고 판단해 실제 브라우저 확인으로 대체했다.
