package com.kirin.superservice.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    INVALID_REQUEST(400, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),

    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "예상하지 못한 서버 오류가 발생했습니다.");

    private final int statusCode;
    private final String code;
    private final String message;

    ErrorCode(int statusCode, String code, String message) {
        this.statusCode = statusCode;
        this.code = code;
        this.message = message;
    }
}
