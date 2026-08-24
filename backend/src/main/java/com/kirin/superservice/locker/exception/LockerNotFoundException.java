package com.kirin.superservice.locker.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class LockerNotFoundException extends BusinessException {
    public LockerNotFoundException(Long lockerId) {
        super(ErrorCode.LOCKER_NOT_FOUND, "물품보관함을 찾을 수 없습니다 - lockerId=" + lockerId);
    }
}
