package com.kirin.superservice.image.exception;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

public class ImageStorageException extends BusinessException {
    public ImageStorageException(String fileName, Throwable cause) {
        super(ErrorCode.IMAGE_UPLOAD_FAILED, "이미지 저장에 실패했습니다 - fileName=" + fileName);
        initCause(cause);
    }
}
