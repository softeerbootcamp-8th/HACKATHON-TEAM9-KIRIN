package com.kirin.superservice.product.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(Long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "물품을 찾을 수 없습니다 - productId=" + productId);
    }
}
