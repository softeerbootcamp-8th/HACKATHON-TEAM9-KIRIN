package com.kirin.superservice.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive Long price,
        @Size(max = 1000) String description,
        @Size(max = 500) String imageUrl,
        @NotBlank @Size(max = 50) String sellerName
) {
}
