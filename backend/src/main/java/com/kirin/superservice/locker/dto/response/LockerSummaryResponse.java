package com.kirin.superservice.locker.dto.response;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.domain.UsageStatus;

public record LockerSummaryResponse(Long lockerId, LockStatus lockStatus, UsageStatus usageStatus) {
    public static LockerSummaryResponse fromEntity(Locker locker) {
        return new LockerSummaryResponse(locker.getId(), locker.getLockStatus(), locker.getUsageStatus());
    }
}
