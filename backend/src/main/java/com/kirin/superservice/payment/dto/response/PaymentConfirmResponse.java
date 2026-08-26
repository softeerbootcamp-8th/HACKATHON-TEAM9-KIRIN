package com.kirin.superservice.payment.dto.response;

import com.kirin.superservice.payment.client.TossConfirmResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        Long amount,
        String approvedAt
) {
    public static PaymentConfirmResponse fromTossResponse(TossConfirmResponse tossResponse) {
        return new PaymentConfirmResponse(
                tossResponse.paymentKey(),
                tossResponse.orderId(),
                tossResponse.status(),
                tossResponse.totalAmount(),
                tossResponse.approvedAt()
        );
    }

    /** 데모용 간편결제: 토스 승인 API를 호출하지 않고 가짜 승인 결과를 만든다. */
    public static PaymentConfirmResponse forDemo(Long amount, LocalDateTime approvedAt) {
        String demoId = UUID.randomUUID().toString();
        return new PaymentConfirmResponse(
                "demo-payment-" + demoId, "demo-order-" + demoId, "DONE", amount, approvedAt.toString());
    }
}
