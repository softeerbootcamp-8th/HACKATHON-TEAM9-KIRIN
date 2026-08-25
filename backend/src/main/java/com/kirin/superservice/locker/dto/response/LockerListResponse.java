package com.kirin.superservice.locker.dto.response;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.product.domain.Product;
import java.util.List;
import java.util.Map;

public record LockerListResponse(List<LockerSummaryResponse> lockers) {

    public static LockerListResponse of(
            List<Locker> lockers, Map<Long, Product> productsByLockerId, Long loginMemberId) {
        return new LockerListResponse(lockers.stream()
                .map(locker -> LockerSummaryResponse.of(
                        locker, productsByLockerId.get(locker.getId()), loginMemberId))
                .toList());
    }
}
