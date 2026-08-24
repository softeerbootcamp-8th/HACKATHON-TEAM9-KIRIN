package com.kirin.superservice.locker.dto.request;

import com.kirin.superservice.locker.domain.LockStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeLockStatusRequest(
        @NotNull LockStatus lockStatus
) {
}
