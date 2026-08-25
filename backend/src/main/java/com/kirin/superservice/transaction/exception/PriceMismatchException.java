package com.kirin.superservice.transaction.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class PriceMismatchException extends BusinessException {
    public PriceMismatchException(Long productId, Long price, Long amount) {
        super(ErrorCode.PRICE_MISMATCH,
                "결제 금액이 물품 가격과 일치하지 않습니다 - productId=" + productId
                        + ", price=" + price + ", amount=" + amount);
    }
}
