package com.kirin.superservice.product.dto.response;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;

public record ProductSummaryResponse(
        Long productId,
        Long lockerId,
        String name,
        Long price,
        String imageUrl,
        ProductStatus status
) {
    public static ProductSummaryResponse fromEntity(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getLockerId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getStatus()
        );
    }
}
