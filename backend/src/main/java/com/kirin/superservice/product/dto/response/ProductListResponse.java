package com.kirin.superservice.product.dto.response;

import com.kirin.superservice.product.domain.Product;
import java.util.List;

public record ProductListResponse(List<ProductSummaryResponse> products) {
    public static ProductListResponse fromEntities(List<Product> products) {
        return new ProductListResponse(products.stream()
                .map(ProductSummaryResponse::fromEntity)
                .toList());
    }
}
