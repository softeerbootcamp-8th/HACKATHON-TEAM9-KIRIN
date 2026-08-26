package com.kirin.superservice.product.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReserveLockerRequest(
        @NotNull Long lockerId,
        Boolean sellImmediately
) {
    public ReserveLockerRequest {
        if (sellImmediately == null) {
            sellImmediately = false;
        }
    }

    public ReserveLockerRequest(Long lockerId) {
        this(lockerId, false);
    }
}
