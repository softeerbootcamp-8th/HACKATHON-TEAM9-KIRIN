package com.kirin.superservice.product.dto.response;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        Long lockerId,
        String name,
        Long price,
        String description,
        String imageUrl,
        String sellerName,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime reservedAt,
        LocalDateTime reservationExpiresAt,
        LocalDateTime depositStartedAt,
        LocalDateTime sellingStartedAt,
        LocalDateTime sellingExpiresAt,
        LocalDateTime recoveryStartedAt
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getLockerId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getSellerName(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getReservedAt(),
                product.getReservationExpiresAt(),
                product.getDepositStartedAt(),
                product.getSellingStartedAt(),
                product.getSellingExpiresAt(),
                product.getRecoveryStartedAt()
        );
    }
}
