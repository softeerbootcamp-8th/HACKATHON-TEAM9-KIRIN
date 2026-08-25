package com.kirin.superservice.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.domain.UsageStatus;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.exception.ProductNotSellingException;
import com.kirin.superservice.product.service.ProductService;
import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.domain.TransactionStatus;
import com.kirin.superservice.transaction.dto.request.PurchaseProductRequest;
import com.kirin.superservice.transaction.exception.PriceMismatchException;
import com.kirin.superservice.transaction.exception.TransactionNotFoundException;
import com.kirin.superservice.transaction.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    ProductService productService;

    @Mock
    LockerService lockerService;

    @InjectMocks
    TransactionService transactionService;

    private Product 물품(ProductStatus status) {
        return new Product(1L, 1L, "아이패드", 300000L, "상태 좋음", null, "원기",
                status, LocalDateTime.now());
    }

    private PurchaseProductRequest 구매요청() {
        return new PurchaseProductRequest(1L, "지훈", "payment_key_1", "order_1", 300000L);
    }

    private PaymentConfirmResponse 결제응답() {
        return new PaymentConfirmResponse("payment_key_1", "order_1", "DONE", 300000L,
                "2026-08-25T12:00:00+09:00");
    }

    private Transaction 거래(TransactionStatus status) {
        return new Transaction(1L, 1L, 1L, "지훈", 300000L, "payment_key_1", "order_1",
                "2026-08-25T12:00:00+09:00", status, LocalDateTime.now());
    }

    @Test
    void 판매중인_물품을_적정_금액으로_구매하려_하면_검증을_통과한다() {
        // given
        given(productService.getProduct(1L)).willReturn(물품(ProductStatus.SELLING));

        // when & then
        transactionService.validatePurchasable(1L, 300000L);
    }

    @Test
    void 판매중이_아닌_물품을_구매하려_하면_예외가_발생한다() {
        // given
        given(productService.getProduct(1L)).willReturn(물품(ProductStatus.SOLD));

        // when & then
        assertThatThrownBy(() -> transactionService.validatePurchasable(1L, 300000L))
                .isInstanceOf(ProductNotSellingException.class);
    }

    @Test
    void 결제금액이_물품가격과_다르면_예외가_발생한다() {
        // given
        given(productService.getProduct(1L)).willReturn(물품(ProductStatus.SELLING));

        // when & then
        assertThatThrownBy(() -> transactionService.validatePurchasable(1L, 1000L))
                .isInstanceOf(PriceMismatchException.class);
    }

    @Test
    void 결제_승인_결과를_반영하면_거래가_결제완료_상태로_저장된다() {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED);
        given(productService.getProductForUpdate(1L)).willReturn(물품(ProductStatus.SELLING));
        given(lockerService.getLocker(1L)).willReturn(locker);
        given(transactionRepository.save(any(Transaction.class)))
                .willReturn(거래(TransactionStatus.PAID));

        // when
        Transaction result = transactionService.completePurchase(구매요청(), 결제응답());

        // then
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PAID);
        assertThat(result.getPaymentKey()).isEqualTo("payment_key_1");
    }

    @Test
    void 결제_승인_결과를_반영하면_물품이_판매완료가_되고_보관함이_열린다() {
        // given
        Product product = 물품(ProductStatus.SELLING);
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED);
        given(productService.getProductForUpdate(1L)).willReturn(product);
        given(lockerService.getLocker(1L)).willReturn(locker);
        given(transactionRepository.save(any(Transaction.class)))
                .willReturn(거래(TransactionStatus.PAID));

        // when
        transactionService.completePurchase(구매요청(), 결제응답());

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.UNLOCKED);
    }

    @Test
    void 잠금_조회_시점에_이미_판매완료된_물품이면_예외가_발생한다() {
        // given
        given(productService.getProductForUpdate(1L)).willReturn(물품(ProductStatus.SOLD));

        // when & then
        assertThatThrownBy(() -> transactionService.completePurchase(구매요청(), 결제응답()))
                .isInstanceOf(ProductNotSellingException.class);
    }

    @Test
    void 수령을_확인하면_거래가_완료되고_보관함이_잠기며_사용가능해진다() {
        // given
        Locker locker = new Locker(1L, LockStatus.UNLOCKED, UsageStatus.OCCUPIED);
        given(transactionRepository.findById(1L)).willReturn(Optional.of(거래(TransactionStatus.PAID)));
        given(lockerService.getLocker(1L)).willReturn(locker);

        // when
        Transaction result = transactionService.completePickup(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.DONE);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
    }

    @Test
    void 이미_수령완료된_거래의_수령을_확인해도_완료_상태가_유지된다() {
        // given
        given(transactionRepository.findById(1L)).willReturn(Optional.of(거래(TransactionStatus.DONE)));

        // when
        Transaction result = transactionService.completePickup(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.DONE);
    }

    @Test
    void 존재하지_않는_거래를_조회하면_예외가_발생한다() {
        // given
        given(transactionRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transactionService.getTransaction(999L))
                .isInstanceOf(TransactionNotFoundException.class);
    }
}
