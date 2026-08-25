package com.kirin.superservice.product.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class SellerMismatchException extends BusinessException {

    public SellerMismatchException(Long productId) {
        super(ErrorCode.SELLER_MISMATCH, "물품을 등록한 판매자가 아닙니다. productId=" + productId);
    }
}
