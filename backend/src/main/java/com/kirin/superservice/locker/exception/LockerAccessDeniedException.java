package com.kirin.superservice.locker.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class LockerAccessDeniedException extends BusinessException {

    public LockerAccessDeniedException(Long lockerId) {
        super(ErrorCode.LOCKER_ACCESS_DENIED, "판매 중인 물품이 없는 물품보관함입니다 - lockerId=" + lockerId);
    }
}
