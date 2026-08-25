package com.kirin.superservice.image.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class InvalidImageFileException extends BusinessException {
    public InvalidImageFileException(String reason) {
        super(ErrorCode.INVALID_IMAGE_FILE, "이미지 파일이 올바르지 않습니다 - reason=" + reason);
    }
}
