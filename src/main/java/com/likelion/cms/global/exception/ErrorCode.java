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
    ATTENDANCE_ALREADY_FINALIZED(HttpStatus.CONFLICT, "C009", "이미 처리된 출결이라 코드로 인증할 수 없습니다."),

    // File
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "F001", "파일 크기 제한을 초과했습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "F002", "지원하지 않는 파일 형식입니다."),
    FILE_UPLOAD_NOT_FOUND(HttpStatus.NOT_FOUND, "F003", "업로드된 파일을 찾을 수 없습니다."),
    FILE_METADATA_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "F004", "업로드된 파일 정보가 요청과 일치하지 않습니다."),
    FILE_STORAGE_ERROR(HttpStatus.BAD_GATEWAY, "F005", "파일 저장소 연동에 실패했습니다."),
    FILE_IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "F006", "동일한 멱등 키의 파일 요청이 이미 처리 중이거나 다른 요청에 사용되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
