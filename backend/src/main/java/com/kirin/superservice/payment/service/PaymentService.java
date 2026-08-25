package com.kirin.superservice.payment.service;

import com.kirin.superservice.payment.client.TossConfirmResponse;
import com.kirin.superservice.payment.client.TossPaymentClient;
import com.kirin.superservice.payment.dto.request.PaymentConfirmRequest;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TossPaymentClient tossPaymentClient;

    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        TossConfirmResponse tossResponse = tossPaymentClient.confirmPayment(
                request.paymentKey(), request.orderId(), request.amount());
        log.info("결제 승인 완료 - orderId={}, paymentKey={}, amount={}",
                request.orderId(), request.paymentKey(), request.amount());
        return PaymentConfirmResponse.fromTossResponse(tossResponse);
    }
}
