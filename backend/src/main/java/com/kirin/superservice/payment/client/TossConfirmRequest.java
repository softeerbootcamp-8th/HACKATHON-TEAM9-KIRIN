package com.kirin.superservice.payment.client;

/**
 * 토스페이먼츠 결제 승인 API(POST /v1/payments/confirm) 요청 본문.
 * 필드명은 토스 API 스펙을 그대로 따른다.
 */
record TossConfirmRequest(String paymentKey, String orderId, Long amount) {
}
