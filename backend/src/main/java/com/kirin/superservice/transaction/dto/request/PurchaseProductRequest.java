package com.kirin.superservice.transaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * amount는 토스 결제위젯이 리다이렉트로 돌려주는 이름을 그대로 받는다. 내부에서는 거래 금액(price)으로 저장한다.
 */
public record PurchaseProductRequest(
        @NotNull Long productId,
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @NotNull @Positive Long amount
) {
}
