package com.likelion.cms.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<ErrorDetail> errors;

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<ErrorDetail> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors);
    }
}
