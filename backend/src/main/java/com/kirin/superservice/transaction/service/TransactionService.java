package com.kirin.superservice.transaction.service;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.service.MemberService;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.exception.ProductNotSellingException;
import com.kirin.superservice.product.exception.SellingPeriodExpiredException;
import com.kirin.superservice.product.service.ProductService;
import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.domain.TransactionStatus;
import com.kirin.superservice.transaction.dto.request.PurchaseProductRequest;
import com.kirin.superservice.transaction.exception.PriceMismatchException;
import com.kirin.superservice.transaction.exception.TransactionAccessDeniedException;
import com.kirin.superservice.transaction.exception.TransactionNotFoundException;
import com.kirin.superservice.transaction.repository.TransactionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductService productService;
    private final LockerService lockerService;
    private final MemberService memberService;
    private final Clock clock;

    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    /** 본인이 구매한 거래인지 확인한 뒤 조회한다. */
    public Transaction getTransaction(Long transactionId, Long buyerMemberId) {
        Transaction transaction = getTransaction(transactionId);
        validateBuyer(transaction, buyerMemberId);
        return transaction;
    }

    /**
     * 결제를 부르기 전에 살 수 있는 물품인지 확인한다. 이미 팔린 물건에 돈부터 받는 일을 막는다.
     */
    public void validatePurchasable(Long productId, Long amount) {
        Product product = productService.getProduct(productId);
        if (!product.isSelling()) {
            throw new ProductNotSellingException(productId, product.getStatus());
        }
        if (product.isSellingExpiredAt(LocalDateTime.now(clock))) {
            throw new SellingPeriodExpiredException(productId);
        }
        if (!product.getPrice().equals(amount)) {
            throw new PriceMismatchException(productId, product.getPrice(), amount);
        }
    }

    /**
     * 결제 승인 결과를 반영한다. 외부 호출 없이 DB 작업만 하므로 트랜잭션이 짧게 끝난다.
     * 물품을 먼저 잠그고 보관함을 건드린다 — 잠금 순서를 뒤집지 않는다.
     */
    @Transactional
    public Transaction completePurchase(PurchaseProductRequest request, PaymentConfirmResponse payment,
            Long buyerMemberId) {
        Product product = productService.getProductForUpdate(request.productId());
        if (!product.isSelling()) {
            throw new ProductNotSellingException(product.getId(), product.getStatus());
        }
        if (product.isSellingExpiredAt(LocalDateTime.now(clock))) {
            throw new SellingPeriodExpiredException(product.getId());
        }
        product.markSold(LocalDateTime.now(clock));
        lockerService.getLocker(product.getLockerId()).changeLockStatus(LockStatus.UNLOCKED);

        Member buyer = memberService.getById(buyerMemberId);
        Transaction transaction = transactionRepository.save(new Transaction(
                product.getId(),
                product.getLockerId(),
                buyerMemberId,
                buyer.getNickname(),
                request.amount(),
                payment.paymentKey(),
                payment.orderId(),
                payment.approvedAt()));
        log.info("물품 구매 완료 - transactionId={}, productId={}, lockerId={}, orderId={}",
                transaction.getId(), product.getId(), product.getLockerId(), payment.orderId());
        return transaction;
    }

    /**
     * 구매자가 물건을 꺼낸 뒤 호출한다. 보관함을 잠그고 다시 비어 있는 상태로 되돌린다.
     * 버튼을 두 번 눌러도 문제가 없도록 이미 수령완료면 그대로 둔다.
     */
    @Transactional
    public Transaction completePickup(Long transactionId, Long buyerMemberId) {
        Transaction transaction = getTransaction(transactionId, buyerMemberId);
        if (transaction.isDone()) {
            return transaction;
        }
        transaction.completePickup();

        Locker locker = lockerService.getLocker(transaction.getLockerId());
        locker.changeLockStatus(LockStatus.LOCKED);
        locker.release();
        log.info("물품 수령 완료 - transactionId={}, lockerId={}", transactionId, transaction.getLockerId());
        return transaction;
    }

    /**
     * 데모용: 사물함 잠금 버튼만으로 수령 완료를 흉내 낸다. 구매자 본인 확인 없이,
     * 그 사물함에 결제완료(PAID) 상태인 거래가 있으면 바로 수령완료 처리한다.
     */
    @Transactional
    public void completePickupForDemo(Long lockerId) {
        transactionRepository.findByLockerIdAndStatus(lockerId, TransactionStatus.PAID)
                .ifPresent(transaction -> {
                    transaction.completePickup();
                    Locker locker = lockerService.getLockerForUpdate(lockerId);
                    locker.changeLockStatus(LockStatus.LOCKED);
                    locker.release();
                    log.info("데모용 사물함 잠금으로 수령 완료 - transactionId={}, lockerId={}",
                            transaction.getId(), lockerId);
                });
    }

    private void validateBuyer(Transaction transaction, Long buyerMemberId) {
        if (!transaction.isOwnedBy(buyerMemberId)) {
            throw new TransactionAccessDeniedException(transaction.getId());
        }
    }
}
