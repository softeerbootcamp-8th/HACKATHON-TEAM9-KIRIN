package com.kirin.superservice.product.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class SellingPeriodExpiredException extends BusinessException {

    public SellingPeriodExpiredException(Long productId) {
        super(ErrorCode.SELLING_PERIOD_EXPIRED, "물품 판매 기간이 만료되었습니다. productId=" + productId);
    }
}
