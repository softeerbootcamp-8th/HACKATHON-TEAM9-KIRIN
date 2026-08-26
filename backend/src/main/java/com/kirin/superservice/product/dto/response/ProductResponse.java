package com.kirin.superservice.product.dto.response;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long productId,
        Long lockerId,
        String name,
        Long price,
        String description,
        List<String> imageUrls,
        String sellerName,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime reservedAt,
        LocalDateTime reservationExpiresAt,
        LocalDateTime depositStartedAt,
        LocalDateTime sellingStartedAt,
        LocalDateTime sellingExpiresAt,
        LocalDateTime recoveryStartedAt,
        Long sellerCompletedSalesCount
) {
    public static ProductResponse fromEntity(Product product) {
        return fromEntity(product, null);
    }

    /** sellerCompletedSalesCount는 상품 상세 화면에서만 채워 넣는 신뢰 지표라 필요할 때만 넘긴다. */
    public static ProductResponse fromEntity(Product product, Long sellerCompletedSalesCount) {
        return new ProductResponse(
                product.getId(),
                product.getLockerId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrls(),
                product.getSellerName(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getReservedAt(),
                product.getReservationExpiresAt(),
                product.getDepositStartedAt(),
                product.getSellingStartedAt(),
                product.getSellingExpiresAt(),
                product.getRecoveryStartedAt(),
                sellerCompletedSalesCount
        );
    }
}
