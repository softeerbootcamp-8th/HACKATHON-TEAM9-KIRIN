package com.kirin.superservice.product.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(Long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "물품을 찾을 수 없습니다 - productId=" + productId);
    }

    private ProductNotFoundException(String message) {
        super(ErrorCode.PRODUCT_NOT_FOUND, message);
    }

    /** QR로 스캔한 물품보관함에 현재 판매 중인 물품이 없을 때 사용한다. */
    public static ProductNotFoundException byLockerId(Long lockerId) {
        return new ProductNotFoundException("물품보관함에 등록된 물품을 찾을 수 없습니다 - lockerId=" + lockerId);
    }
}
