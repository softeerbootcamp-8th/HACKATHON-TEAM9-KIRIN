package com.kirin.superservice.locker.dto.response;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.domain.UsageStatus;
import com.kirin.superservice.product.domain.Product;
import java.time.LocalDateTime;

public record LockerSummaryResponse(
        Long lockerId,
        LockStatus lockStatus,
        UsageStatus usageStatus,
        boolean isMine,
        LocalDateTime reservationExpiresAt,
        LocalDateTime sellingExpiresAt) {

    /**
     * 로그인한 회원({@code loginMemberId})이 사물함을 점유 중인 물품의 판매자일 때만
     * 실제 사용상태와 남은 시간/날짜 계산용 시각을 노출한다. 그 외에는(비로그인 포함)
     * 예약이든 판매중이든 구분 없이 전부 사용중(OCCUPIED)으로 표시한다.
     */
    public static LockerSummaryResponse of(Locker locker, Product product, Long loginMemberId) {
        boolean isMine = product != null && loginMemberId != null && product.isOwnedBy(loginMemberId);
        UsageStatus usageStatus = isMine ? locker.getUsageStatus() : maskUsageStatus(locker.getUsageStatus());
        LocalDateTime reservationExpiresAt =
                isMine && product.isReserved() ? product.getReservationExpiresAt() : null;
        LocalDateTime sellingExpiresAt =
                isMine && product.isSelling() ? product.getSellingExpiresAt() : null;

        return new LockerSummaryResponse(
                locker.getId(), locker.getLockStatus(), usageStatus, isMine,
                reservationExpiresAt, sellingExpiresAt);
    }

    private static UsageStatus maskUsageStatus(UsageStatus actual) {
        return actual == UsageStatus.AVAILABLE ? UsageStatus.AVAILABLE : UsageStatus.OCCUPIED;
    }
}
