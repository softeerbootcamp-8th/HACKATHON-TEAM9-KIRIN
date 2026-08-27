package com.kirin.superservice.product.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1_000) @Max(1_000_000_000) Long price,
        @NotBlank @Size(min = 10, max = 1000) String description,
        @Size(max = 10) List<String> imageUrls
) {
}
