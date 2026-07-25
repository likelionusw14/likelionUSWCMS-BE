package com.likelion.cms.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C004", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "C006", "리소스 충돌이 발생했습니다."),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "C007", "다른 사용자에 의해 이미 수정되었습니다."),

    // Attendance
    ATTENDANCE_CODE_INVALID(HttpStatus.CONFLICT, "C008", "출결 코드가 유효하지 않거나 만료되었습니다."),
    ATTENDANCE_ALREADY_FINALIZED(HttpStatus.CONFLICT, "C009", "이미 처리된 출결이라 코드로 인증할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
