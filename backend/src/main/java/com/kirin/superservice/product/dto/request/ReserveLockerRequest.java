package com.kirin.superservice.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReserveLockerRequest(
        @NotNull Long lockerId,
        @NotBlank @Size(max = 50) String sellerName
) {
}
