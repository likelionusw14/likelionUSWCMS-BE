package com.likelion.cms.domain.attendance.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void checkInAcceptsSixDigitCode() {
        AttendanceCheckInRequest request = new AttendanceCheckInRequest("123456");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void checkInRejectsNonSixDigitCode() {
        AttendanceCheckInRequest request = new AttendanceCheckInRequest("12a456");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("code");
    }

    @Test
    void checkInRejectsBlankCode() {
        AttendanceCheckInRequest request = new AttendanceCheckInRequest(" ");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("code");
    }
}
