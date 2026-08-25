package com.kirin.superservice.locker.dto.response;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;

public record LockerLockStatusResponse(Long lockerId, LockStatus lockStatus) {
    public static LockerLockStatusResponse fromEntity(Locker locker) {
        return new LockerLockStatusResponse(locker.getId(), locker.getLockStatus());
    }
}
