package com.kirin.superservice.locker.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class LockerNotAvailableException extends BusinessException {

    public LockerNotAvailableException(Long lockerId) {
        super(ErrorCode.LOCKER_NOT_AVAILABLE, "예약하거나 사용할 수 없는 물품보관함입니다. lockerId=" + lockerId);
    }
}
