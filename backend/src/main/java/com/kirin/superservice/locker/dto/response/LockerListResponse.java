package com.kirin.superservice.locker.dto.response;

import com.kirin.superservice.locker.domain.Locker;
import java.util.List;

public record LockerListResponse(List<LockerSummaryResponse> lockers) {
    public static LockerListResponse fromEntities(List<Locker> lockers) {
        return new LockerListResponse(lockers.stream()
                .map(LockerSummaryResponse::fromEntity)
                .toList());
    }
}
