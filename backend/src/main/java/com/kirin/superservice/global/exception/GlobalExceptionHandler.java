package com.kirin.superservice.global.exception;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.kirin.superservice.global.slack.ErrorRequestContext;
import com.kirin.superservice.global.slack.SlackErrorNotifier;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final SlackErrorNotifier slackErrorNotifier;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("비즈니스 예외 발생 - code={}, message={}", errorCode.getCode(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ErrorResponse.of(errorCode.getStatusCode(), errorCode.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describeFieldError)
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = ErrorCode.INVALID_REQUEST.getMessage();
        }
        return handleExceptionInternal(e, message, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String invalidField = JacksonFieldPathResolver.resolve(e);
        log.warn("요청 본문을 읽을 수 없음 - path={}, field={}", resolvePath(request), invalidField, e);

        String message = invalidField == null
                ? "요청 본문 형식이 올바르지 않습니다. 입력값을 확인해주세요."
                : "요청 필드 '%s' 값이 올바르지 않습니다. 입력값을 확인해주세요.".formatted(invalidField);

        return handleExceptionInternal(e, message, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(e, "지원하지 않는 HTTP 메서드입니다: " + e.getMethod(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(e, "지원하지 않는 Content-Type입니다: " + e.getContentType(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(e, "필수 파라미터가 누락되었습니다: " + e.getParameterName(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(e, "파라미터 타입이 올바르지 않습니다: " + e.getPropertyName(), headers, status, request);
    }

    private static String describeFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ErrorCode errorCode = errorCodeFor(statusCode);
        String message = body instanceof String stringBody ? stringBody : errorCode.getMessage();

        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(ErrorResponse.of(statusCode.value(), errorCode.getCode(), message, resolvePath(request)));
    }

    private ErrorCode errorCodeFor(HttpStatusCode statusCode) {
        if (statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return ErrorCode.RESOURCE_NOT_FOUND;
        }
        if (statusCode.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (statusCode.isSameCodeAs(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        return ErrorCode.INVALID_REQUEST;
    }

    private String resolvePath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("예상하지 못한 런타임 예외 발생 - path={}", request.getRequestURI(), e);

        ErrorRequestContext context = ErrorRequestContext.from(request, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        slackErrorNotifier.notifyError(e, context);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }
}
