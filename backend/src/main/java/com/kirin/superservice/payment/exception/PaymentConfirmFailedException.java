package com.kirin.superservice.payment.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class PaymentConfirmFailedException extends BusinessException {
    public PaymentConfirmFailedException(String orderId, String reason) {
        super(ErrorCode.PAYMENT_CONFIRM_FAILED,
                "결제 승인에 실패했습니다 - orderId=" + orderId + ", reason=" + reason);
    }
}
