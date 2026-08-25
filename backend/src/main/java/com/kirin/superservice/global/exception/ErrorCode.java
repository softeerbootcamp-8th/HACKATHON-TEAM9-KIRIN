package com.kirin.superservice.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    INVALID_REQUEST(400, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "로그인이 필요합니다."),
    INVALID_PASSWORD(401, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),
    DUPLICATE_LOGIN_ID(409, "DUPLICATE_LOGIN_ID", "이미 사용 중인 로그인 아이디입니다."),
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),

    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "예상하지 못한 서버 오류가 발생했습니다."),

    LOCKER_NOT_FOUND(404, "LOCKER_NOT_FOUND", "물품보관함을 찾을 수 없습니다."),
    LOCKER_ALREADY_OCCUPIED(409, "LOCKER_ALREADY_OCCUPIED", "이미 사용 중인 물품보관함입니다."),
    LOCKER_NOT_AVAILABLE(409, "LOCKER_NOT_AVAILABLE", "예약하거나 사용할 수 없는 물품보관함입니다."),
    PAYMENT_CONFIRM_FAILED(400, "PAYMENT_CONFIRM_FAILED", "결제 승인에 실패했습니다."),
    PRODUCT_NOT_FOUND(404, "PRODUCT_NOT_FOUND", "물품을 찾을 수 없습니다."),
    SELLER_MISMATCH(403, "SELLER_MISMATCH", "물품을 등록한 판매자가 아닙니다."),
    RESERVATION_EXPIRED(409, "RESERVATION_EXPIRED", "사물함 예약 시간이 만료되었습니다."),
    SELLING_PERIOD_EXPIRED(409, "SELLING_PERIOD_EXPIRED", "물품 판매 기간이 만료되었습니다."),
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
