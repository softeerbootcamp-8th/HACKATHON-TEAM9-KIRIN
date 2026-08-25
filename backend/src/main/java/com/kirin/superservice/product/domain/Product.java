package com.kirin.superservice.product.domain;

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
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "locker_id", nullable = false)
    private Long lockerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "seller_name", nullable = false, length = 50)
    private String sellerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Product(Long id, Long lockerId, String name, Long price, String description,
            String imageUrl, String sellerName, ProductStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.lockerId = lockerId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sellerName = sellerName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Product(Long lockerId, String name, Long price, String description,
            String imageUrl, String sellerName) {
        this(null, lockerId, name, price, description, imageUrl, sellerName,
                ProductStatus.PREPARING, LocalDateTime.now());
    }

    public void startSelling() {
        this.status = ProductStatus.SELLING;
    }

    public void markSold() {
        this.status = ProductStatus.SOLD;
    }

    public boolean isPreparing() {
        return this.status == ProductStatus.PREPARING;
    }

    public boolean isSelling() {
        return this.status == ProductStatus.SELLING;
    }

    public boolean isSold() {
        return this.status == ProductStatus.SOLD;
    }
}
