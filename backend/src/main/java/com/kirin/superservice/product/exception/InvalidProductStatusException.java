package com.kirin.superservice.product.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;
import com.kirin.superservice.product.domain.ProductStatus;

public class InvalidProductStatusException extends BusinessException {
    public InvalidProductStatusException(Long productId, ProductStatus status) {
        super(ErrorCode.INVALID_PRODUCT_STATUS,
                "현재 물품 상태에서는 처리할 수 없는 요청입니다 - productId=" + productId + ", status=" + status);
    }
}
