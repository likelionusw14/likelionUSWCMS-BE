package com.likelion.cms.domain.attendance.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.likelion.cms.domain.attendance.entity.AdminSettableAttendanceStatus;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UpdateAttendanceRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("memo를 아예 안 보내면 유효하다 (기존 값 유지 의도)")
    void valid_withoutMemo() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.PRESENT);
        request.setVersion(0);

        Set<ConstraintViolation<UpdateAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("memo를 null로 명시하면 유효하다 (삭제 의도)")
    void valid_memoExplicitNull() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.LATE);
        request.setMemo(null);
        request.setVersion(1);

        Set<ConstraintViolation<UpdateAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("memo에 값이 있으면 유효하다 (수정 의도)")
    void valid_memoWithValue() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.ABSENT);
        request.setMemo("버스 지연");
        request.setVersion(2);

        Set<ConstraintViolation<UpdateAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("status가 null이면 유효하지 않다")
    void invalid_statusNull() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setVersion(0);

        Set<ConstraintViolation<UpdateAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("version이 null이면 유효하지 않다")
    void invalid_versionNull() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.PRESENT);

        Set<ConstraintViolation<UpdateAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("version이 음수면 유효하지 않다")
    void invalid_versionNegative() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.PRESENT);
        request.setVersion(-1);

        Set<ConstraintViolation<UpdateAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("memo를 명시했는데 공백뿐이면 유효하지 않다")
    void invalid_memoBlank() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.PRESENT);
        request.setMemo("   ");
        request.setVersion(0);

        Set<ConstraintViolation<UpdateAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("memo가 1000자를 초과하면 유효하지 않다")
    void invalid_memoTooLong() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.PRESENT);
        request.setMemo("a".repeat(1001));
        request.setVersion(0);

        Set<ConstraintViolation<UpdateAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}