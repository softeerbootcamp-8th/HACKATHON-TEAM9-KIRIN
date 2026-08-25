package com.kirin.superservice.payment.dto.response;

import com.kirin.superservice.payment.client.TossConfirmResponse;

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
}
