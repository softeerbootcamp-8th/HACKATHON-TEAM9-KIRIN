package com.kirin.superservice.transaction.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class TransactionNotFoundException extends BusinessException {
    public TransactionNotFoundException(Long transactionId) {
        super(ErrorCode.TRANSACTION_NOT_FOUND, "거래를 찾을 수 없습니다 - transactionId=" + transactionId);
    }
}
