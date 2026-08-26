package com.kirin.superservice.transaction.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 데모용 간편결제 요청. 토스 결제위젯을 거치지 않으므로 paymentKey·orderId·amount 없이
 * productId만 받는다. 결제 금액은 서버가 물품의 현재 가격에서 직접 가져온다.
 */
public record PurchaseProductForDemoRequest(
        @NotNull Long productId
) {
}
