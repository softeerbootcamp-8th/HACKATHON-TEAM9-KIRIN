package com.kirin.superservice.payment.client;

/**
 * 토스페이먼츠 결제 승인 API 응답 중 우리 서비스에 필요한 필드만 담는다.
 * 필드명은 토스 API 스펙을 그대로 따른다.
 */
public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        Long totalAmount,
        String method,
        String approvedAt
) {
}
