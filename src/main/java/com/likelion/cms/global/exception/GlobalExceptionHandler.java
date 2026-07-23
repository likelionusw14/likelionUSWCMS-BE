package com.likelion.cms.global.exception;

import com.likelion.cms.global.response.ErrorDetail;
import com.likelion.cms.global.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        List<ErrorDetail> errorDetails = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorDetail.of(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, errorDetails));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class
    })
    protected ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
        log.warn("Invalid request: {}", e.getClass().getSimpleName());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    protected ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException e
    ) {
        log.warn("Optimistic locking conflict: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.OPTIMISTIC_LOCK_CONFLICT;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}
