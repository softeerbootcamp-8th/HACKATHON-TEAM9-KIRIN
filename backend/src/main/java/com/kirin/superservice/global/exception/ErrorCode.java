package com.kirin.superservice.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    LOCKER_NOT_FOUND(404, "LOCKER_NOT_FOUND", "물품보관함을 찾을 수 없습니다."),
    INVALID_REQUEST(400, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    PAYMENT_CONFIRM_FAILED(400, "PAYMENT_CONFIRM_FAILED", "결제 승인에 실패했습니다."),
    LOCKER_ALREADY_OCCUPIED(409, "LOCKER_ALREADY_OCCUPIED", "이미 사용 중인 물품보관함입니다."),
    PRODUCT_NOT_FOUND(404, "PRODUCT_NOT_FOUND", "물품을 찾을 수 없습니다."),
    PRODUCT_NOT_SELLING(409, "PRODUCT_NOT_SELLING", "판매 중인 물품이 아닙니다."),
    INVALID_PRODUCT_STATUS(409, "INVALID_PRODUCT_STATUS", "현재 물품 상태에서는 처리할 수 없는 요청입니다."),
    TRANSACTION_NOT_FOUND(404, "TRANSACTION_NOT_FOUND", "거래를 찾을 수 없습니다."),
    PRICE_MISMATCH(400, "PRICE_MISMATCH", "결제 금액이 물품 가격과 일치하지 않습니다."),
    PURCHASE_COMPLETION_FAILED(500, "PURCHASE_COMPLETION_FAILED", "결제는 완료되었으나 거래 처리에 실패했습니다.");

    private final int statusCode;
    private final String code;
    private final String message;

    ErrorCode(int statusCode, String code, String message) {
        this.statusCode = statusCode;
        this.code = code;
        this.message = message;
    }
}
