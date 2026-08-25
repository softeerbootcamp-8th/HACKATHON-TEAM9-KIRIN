package com.kirin.superservice.transaction.service;

import com.kirin.superservice.payment.dto.request.PaymentConfirmRequest;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import com.kirin.superservice.payment.service.PaymentService;
import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.dto.request.PurchaseProductRequest;
import com.kirin.superservice.transaction.exception.PurchaseCompletionFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 구매 흐름을 엮는 역할만 한다. 토스 승인은 HTTP 호출이라 DB 트랜잭션 밖에서 처리해야 하므로
 * 이 클래스에는 {@code @Transactional}을 붙이지 않고, DB 작업은 별도 빈인 {@link TransactionService}에 맡긴다.
 * 같은 클래스 안에서 나눴다면 자기 호출이라 프록시를 타지 못해 트랜잭션이 걸리지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PaymentService paymentService;
    private final TransactionService transactionService;

    public Transaction purchaseProduct(PurchaseProductRequest request) {
        transactionService.validatePurchasable(request.productId(), request.amount());

        PaymentConfirmResponse payment = paymentService.confirmPayment(new PaymentConfirmRequest(
                request.paymentKey(), request.orderId(), request.amount()));

        try {
            return transactionService.completePurchase(request, payment);
        } catch (Exception e) {
            // 돈은 빠져나갔는데 거래가 남지 않은 상태다. 환불에 필요한 값을 전부 남긴다.
            log.error("결제는 승인됐으나 거래 처리에 실패 - orderId={}, paymentKey={}, productId={}",
                    request.orderId(), request.paymentKey(), request.productId(), e);
            throw new PurchaseCompletionFailedException(request.orderId(), request.paymentKey());
        }
    }
}
