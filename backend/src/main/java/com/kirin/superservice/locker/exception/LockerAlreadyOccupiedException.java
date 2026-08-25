package com.kirin.superservice.locker.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class LockerAlreadyOccupiedException extends BusinessException {
    public LockerAlreadyOccupiedException(Long lockerId) {
        super(ErrorCode.LOCKER_ALREADY_OCCUPIED, "이미 사용 중인 물품보관함입니다 - lockerId=" + lockerId);
    }
}
