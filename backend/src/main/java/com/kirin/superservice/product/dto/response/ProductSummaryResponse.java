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
    /** 목록·그리드용 썸네일이라 여러 장 중 첫 장만 대표로 내려준다. */
    public static ProductSummaryResponse fromEntity(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getLockerId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrls().isEmpty() ? null : product.getImageUrls().get(0),
                product.getStatus()
        );
    }
}
