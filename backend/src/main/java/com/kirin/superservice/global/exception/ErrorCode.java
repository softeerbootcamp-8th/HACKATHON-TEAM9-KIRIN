package com.kirin.superservice.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    LOCKER_NOT_FOUND(404, "LOCKER_NOT_FOUND", "물품보관함을 찾을 수 없습니다."),
    INVALID_REQUEST(400, "INVALID_REQUEST", "요청 값이 올바르지 않습니다.");

    private final int statusCode;
    private final String code;
    private final String message;

    ErrorCode(int statusCode, String code, String message) {
        this.statusCode = statusCode;
        this.code = code;
        this.message = message;
    }
}
