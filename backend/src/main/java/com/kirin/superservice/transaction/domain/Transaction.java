package com.kirin.superservice.transaction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 결제 시점의 보관함을 스냅샷으로 남긴다. 보관함이 나중에 다른 물품에 재사용돼도 이 거래의 이력은 유지된다.
     */
    @Column(name = "locker_id", nullable = false)
    private Long lockerId;

    @Column(name = "buyer_member_id", nullable = false)
    private Long buyerMemberId;

    @Column(name = "buyer_name", nullable = false, length = 50)
    private String buyerName;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "payment_key", nullable = false, length = 200, unique = true)
    private String paymentKey;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "approved_at", length = 50)
    private String approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Transaction(Long id, Long productId, Long lockerId, Long buyerMemberId, String buyerName,
            Long price, String paymentKey, String orderId, String approvedAt, TransactionStatus status,
            LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.lockerId = lockerId;
        this.buyerMemberId = buyerMemberId;
        this.buyerName = buyerName;
        this.price = price;
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.approvedAt = approvedAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Transaction(Long productId, Long lockerId, Long buyerMemberId, String buyerName, Long price,
            String paymentKey, String orderId, String approvedAt) {
        this(null, productId, lockerId, buyerMemberId, buyerName, price, paymentKey, orderId, approvedAt,
                TransactionStatus.PAID, LocalDateTime.now());
    }

    public void completePickup() {
        this.status = TransactionStatus.DONE;
    }

    public boolean isDone() {
        return this.status == TransactionStatus.DONE;
    }

    public boolean isOwnedBy(Long buyerMemberId) {
        return this.buyerMemberId.equals(buyerMemberId);
    }
}
