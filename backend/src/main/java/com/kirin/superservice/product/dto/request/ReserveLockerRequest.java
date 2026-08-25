package com.kirin.superservice.product.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReserveLockerRequest(
        @NotNull Long lockerId
) {
}
