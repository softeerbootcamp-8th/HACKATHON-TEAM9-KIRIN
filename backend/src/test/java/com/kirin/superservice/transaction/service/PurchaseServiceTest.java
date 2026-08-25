package com.kirin.superservice.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.kirin.superservice.payment.dto.request.PaymentConfirmRequest;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import com.kirin.superservice.payment.exception.PaymentConfirmFailedException;
import com.kirin.superservice.payment.service.PaymentService;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.exception.ProductNotSellingException;
import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.domain.TransactionStatus;
import com.kirin.superservice.transaction.dto.request.PurchaseProductRequest;
import com.kirin.superservice.transaction.exception.PurchaseCompletionFailedException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    PaymentService paymentService;

    @Mock
    TransactionService transactionService;

    @InjectMocks
    PurchaseService purchaseService;

    private static final Long 구매자_ID = 1L;

    private PurchaseProductRequest 구매요청() {
        return new PurchaseProductRequest(1L, "payment_key_1", "order_1", 300000L);
    }

    private PaymentConfirmResponse 결제응답() {
        return new PaymentConfirmResponse("payment_key_1", "order_1", "DONE", 300000L,
                "2026-08-25T12:00:00+09:00");
    }

    private Transaction 거래() {
        return new Transaction(1L, 1L, 1L, 구매자_ID, "지훈", 300000L, "payment_key_1", "order_1",
                "2026-08-25T12:00:00+09:00", TransactionStatus.PAID, LocalDateTime.now());
    }

    @Test
    void 결제_승인에_성공하면_거래가_저장되고_거래정보를_반환한다() {
        // given
        given(paymentService.confirmPayment(any(PaymentConfirmRequest.class))).willReturn(결제응답());
        given(transactionService.completePurchase(any(PurchaseProductRequest.class),
                any(PaymentConfirmResponse.class), any(Long.class))).willReturn(거래());

        // when
        Transaction result = purchaseService.purchaseProduct(구매요청(), 구매자_ID);

        // then
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PAID);
        assertThat(result.getOrderId()).isEqualTo("order_1");
    }

    @Test
    void 결제_승인에_실패하면_거래가_저장되지_않는다() {
        // given
        given(paymentService.confirmPayment(any(PaymentConfirmRequest.class)))
                .willThrow(new PaymentConfirmFailedException("order_1", "카드 한도 초과"));

        // when & then
        assertThatThrownBy(() -> purchaseService.purchaseProduct(구매요청(), 구매자_ID))
                .isInstanceOf(PaymentConfirmFailedException.class);
        then(transactionService).should(never())
                .completePurchase(any(PurchaseProductRequest.class), any(PaymentConfirmResponse.class), any(Long.class));
    }

    @Test
    void 판매중이_아닌_물품이면_결제를_시도하지_않는다() {
        // given
        willThrow(new ProductNotSellingException(1L, ProductStatus.SOLD))
                .given(transactionService).validatePurchasable(1L, 300000L);

        // when & then
        assertThatThrownBy(() -> purchaseService.purchaseProduct(구매요청(), 구매자_ID))
                .isInstanceOf(ProductNotSellingException.class);
        then(paymentService).should(never()).confirmPayment(any(PaymentConfirmRequest.class));
    }

    @Test
    void 결제는_승인됐지만_거래_저장에_실패하면_구매처리실패_예외가_발생한다() {
        // given
        given(paymentService.confirmPayment(any(PaymentConfirmRequest.class))).willReturn(결제응답());
        given(transactionService.completePurchase(any(PurchaseProductRequest.class),
                any(PaymentConfirmResponse.class), any(Long.class))).willThrow(new IllegalStateException("DB 오류"));

        // when & then
        assertThatThrownBy(() -> purchaseService.purchaseProduct(구매요청(), 구매자_ID))
                .isInstanceOf(PurchaseCompletionFailedException.class);
    }
}
