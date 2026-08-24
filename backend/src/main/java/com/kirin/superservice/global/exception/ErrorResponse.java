package com.kirin.superservice.global.exception;

public record ErrorResponse(String code, String message) {
    public static ErrorResponse fromErrorCode(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}
