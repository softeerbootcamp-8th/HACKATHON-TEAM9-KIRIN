package com.kirin.superservice.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteDepositRequest(
        @NotBlank @Size(max = 50) String sellerName
) {
}
