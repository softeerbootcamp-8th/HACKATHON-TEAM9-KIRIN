package com.kirin.superservice.product.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;
import com.kirin.superservice.product.domain.ProductStatus;

public class ProductNotSellingException extends BusinessException {
    public ProductNotSellingException(Long productId, ProductStatus status) {
        super(ErrorCode.PRODUCT_NOT_SELLING,
                "판매 중인 물품이 아닙니다 - productId=" + productId + ", status=" + status);
    }
}
