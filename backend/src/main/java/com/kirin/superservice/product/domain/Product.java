package com.kirin.superservice.product.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "locker_id")
    private Long lockerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "description", length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_image", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "seller_member_id", nullable = false)
    private Long sellerMemberId;

    @Column(name = "seller_name", nullable = false, length = 50)
    private String sellerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;

    @Column(name = "deposit_started_at")
    private LocalDateTime depositStartedAt;

    @Column(name = "selling_started_at")
    private LocalDateTime sellingStartedAt;

    @Column(name = "selling_expires_at")
    private LocalDateTime sellingExpiresAt;

    @Column(name = "recovery_started_at")
    private LocalDateTime recoveryStartedAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    public Product(Long id, Long lockerId, String name, Long price, String description,
            List<String> imageUrls, Long sellerMemberId, String sellerName, ProductStatus status,
            LocalDateTime createdAt) {
        this.id = id;
        this.lockerId = lockerId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
        this.sellerMemberId = sellerMemberId;
        this.sellerName = sellerName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Product(String name, Long price, String description, List<String> imageUrls,
            Long sellerMemberId, String sellerName) {
        this(null, null, name, price, description, imageUrls, sellerMemberId, sellerName,
                ProductStatus.PREPARING, LocalDateTime.now());
    }

    public void reserveLocker(Long lockerId, LocalDateTime reservedAt,
            LocalDateTime reservationExpiresAt) {
        this.lockerId = lockerId;
        this.reservedAt = reservedAt;
        this.reservationExpiresAt = reservationExpiresAt;
        this.depositStartedAt = null;
        this.status = ProductStatus.RESERVED;
    }

    public void cancelLockerReservation() {
        this.lockerId = null;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
        this.depositStartedAt = null;
        this.status = ProductStatus.PREPARING;
    }

    public boolean isOwnedBy(Long sellerMemberId) {
        return this.sellerMemberId.equals(sellerMemberId);
    }

    public boolean hasStartedDeposit() {
        return this.depositStartedAt != null;
    }

    public void startDeposit(LocalDateTime depositStartedAt) {
        this.depositStartedAt = depositStartedAt;
    }

    public void completeDeposit(LocalDateTime sellingStartedAt, LocalDateTime sellingExpiresAt) {
        this.sellingStartedAt = sellingStartedAt;
        this.sellingExpiresAt = sellingExpiresAt;
        this.status = ProductStatus.SELLING;
    }

    public boolean isReservationExpiredAt(LocalDateTime now) {
        return this.reservationExpiresAt == null || !now.isBefore(this.reservationExpiresAt);
    }

    public void expireSelling() {
        this.status = ProductStatus.EXPIRED;
    }

    public void startRecovery(LocalDateTime recoveryStartedAt) {
        this.recoveryStartedAt = recoveryStartedAt;
    }

    public void completeRecovery() {
        this.lockerId = null;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
        this.depositStartedAt = null;
        this.sellingStartedAt = null;
        this.sellingExpiresAt = null;
        this.recoveryStartedAt = null;
        this.status = ProductStatus.PREPARING;
    }

    public boolean isSellingExpiredAt(LocalDateTime now) {
        return this.sellingExpiresAt == null || !now.isBefore(this.sellingExpiresAt);
    }

    public boolean hasStartedRecovery() {
        return this.recoveryStartedAt != null;
    }

    public void markSold(LocalDateTime soldAt) {
        this.status = ProductStatus.SOLD;
        this.soldAt = soldAt;
    }

    public boolean isPreparing() {
        return this.status == ProductStatus.PREPARING;
    }

    public boolean isSelling() {
        return this.status == ProductStatus.SELLING;
    }

    public boolean isReserved() {
        return this.status == ProductStatus.RESERVED;
    }

    public boolean isSold() {
        return this.status == ProductStatus.SOLD;
    }

    public boolean isExpired() {
        return this.status == ProductStatus.EXPIRED;
    }
}
