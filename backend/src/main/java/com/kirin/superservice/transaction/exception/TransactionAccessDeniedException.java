package com.kirin.superservice.transaction.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class TransactionAccessDeniedException extends BusinessException {

    public TransactionAccessDeniedException(Long transactionId) {
        super(ErrorCode.TRANSACTION_ACCESS_DENIED, "본인의 거래가 아닙니다. transactionId=" + transactionId);
    }
}
