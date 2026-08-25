package com.kirin.superservice.global.exception;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ErrorResponse(
        int status,
        String code,
        String message,
        String path,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        ZonedDateTime timestamp) {

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return of(errorCode.getStatusCode(), errorCode.getCode(), errorCode.getMessage(), path);
    }

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(status, code, message, path, ZonedDateTime.now(ZoneOffset.UTC));
    }
}
