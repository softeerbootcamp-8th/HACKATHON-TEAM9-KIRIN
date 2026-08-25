package com.kirin.superservice.product.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class ReservationExpiredException extends BusinessException {

    public ReservationExpiredException(Long productId) {
        super(ErrorCode.RESERVATION_EXPIRED, "사물함 예약 시간이 만료되었습니다. productId=" + productId);
    }
}
