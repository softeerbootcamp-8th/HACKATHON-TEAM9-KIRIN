package com.kirin.superservice.transaction.service;

import com.kirin.superservice.payment.dto.request.PaymentConfirmRequest;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import com.kirin.superservice.payment.service.PaymentService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.service.ProductService;
import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.dto.request.PurchaseProductRequest;
import com.kirin.superservice.transaction.exception.PurchaseCompletionFailedException;
import java.time.Clock;
import java.time.LocalDateTime;
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
    private final ProductService productService;
    private final Clock clock;

    public Transaction purchaseProduct(PurchaseProductRequest request, Long buyerMemberId) {
        transactionService.validatePurchasable(request.productId(), request.amount());

        PaymentConfirmResponse payment = paymentService.confirmPayment(new PaymentConfirmRequest(
                request.paymentKey(), request.orderId(), request.amount()));

        try {
            return transactionService.completePurchase(request, payment, buyerMemberId);
        } catch (Exception e) {
            // 돈은 빠져나갔는데 거래가 남지 않은 상태다. 환불에 필요한 값을 전부 남긴다.
            log.error("결제는 승인됐으나 거래 처리에 실패 - orderId={}, paymentKey={}, productId={}",
                    request.orderId(), request.paymentKey(), request.productId(), e);
            throw new PurchaseCompletionFailedException(request.orderId(), request.paymentKey());
        }
    }

    /**
     * 데모용 간편결제: 토스 결제위젯·승인 API를 거치지 않고, 물품의 현재 가격으로 곧바로
     * 거래를 만든다. 결제수단 선택 화면 없이 버튼 한 번으로 구매 흐름을 끝까지 확인하기 위함이다.
     */
    public Transaction purchaseProductForDemo(Long productId, Long buyerMemberId) {
        Product product = productService.getProduct(productId);
        Long amount = product.getPrice();
        transactionService.validatePurchasable(productId, amount);

        PaymentConfirmResponse payment = PaymentConfirmResponse.forDemo(amount, LocalDateTime.now(clock));
        PurchaseProductRequest request = new PurchaseProductRequest(
                productId, payment.paymentKey(), payment.orderId(), amount);

        try {
            return transactionService.completePurchase(request, payment, buyerMemberId);
        } catch (Exception e) {
            log.error("데모용 간편결제 처리 실패 - productId={}, orderId={}", productId, payment.orderId(), e);
            throw new PurchaseCompletionFailedException(payment.orderId(), payment.paymentKey());
        }
    }
}
