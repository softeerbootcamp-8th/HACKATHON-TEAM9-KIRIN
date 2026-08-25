package com.kirin.superservice.transaction.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class PurchaseCompletionFailedException extends BusinessException {
    public PurchaseCompletionFailedException(String orderId, String paymentKey) {
        super(ErrorCode.PURCHASE_COMPLETION_FAILED,
                "결제는 완료되었으나 거래 처리에 실패했습니다 - orderId=" + orderId + ", paymentKey=" + paymentKey);
    }
}
