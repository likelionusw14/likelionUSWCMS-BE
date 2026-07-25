package com.likelion.cms.domain.schedule.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("모든 필수 값이 유효하면 CreateScheduleRequest 검증을 통과한다")
    void createRequest_valid() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션",
                "설명입니다",
                1L,
                LocalDate.of(2026, 8, 1),
                false,
                LocalTime.of(19, 0),
                "본관 101호"
        );

        Set<ConstraintViolation<CreateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("title이 비어있으면 CreateScheduleRequest 검증에 실패한다")
    void createRequest_blankTitle() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "",
                null,
                1L,
                LocalDate.of(2026, 8, 1),
                false,
                LocalTime.of(19, 0),
                null
        );

        Set<ConstraintViolation<CreateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("title이 150자를 초과하면 CreateScheduleRequest 검증에 실패한다")
    void createRequest_titleTooLong() {
        String longTitle = "가".repeat(151);
        CreateScheduleRequest request = new CreateScheduleRequest(
                longTitle,
                null,
                1L,
                LocalDate.of(2026, 8, 1),
                false,
                LocalTime.of(19, 0),
                null
        );

        Set<ConstraintViolation<CreateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("cohortId가 없으면 CreateScheduleRequest 검증에 실패한다")
    void createRequest_missingCohortId() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션",
                null,
                null,
                LocalDate.of(2026, 8, 1),
                false,
                LocalTime.of(19, 0),
                null
        );

        Set<ConstraintViolation<CreateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("scheduleDate가 없으면 CreateScheduleRequest 검증에 실패한다")
    void createRequest_missingScheduleDate() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션",
                null,
                1L,
                null,
                false,
                LocalTime.of(19, 0),
                null
        );

        Set<ConstraintViolation<CreateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("isAllDay가 없으면 CreateScheduleRequest 검증에 실패한다")
    void createRequest_missingIsAllDay() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션",
                null,
                1L,
                LocalDate.of(2026, 8, 1),
                null,
                LocalTime.of(19, 0),
                null
        );

        Set<ConstraintViolation<CreateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("version이 없으면 UpdateScheduleRequest 검증에 실패한다")
    void updateRequest_missingVersion() throws Exception {
        UpdateScheduleRequest request = fromJson("""
                {"title": "변경된 제목"}
                """);

        Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("version 외에 아무 필드도 없으면 UpdateScheduleRequest 검증에 실패한다 (minProperties)")
    void updateRequest_onlyVersion_fails() throws Exception {
        UpdateScheduleRequest request = fromJson("""
                {"version": 1}
                """);

        Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("version과 title만 있어도 UpdateScheduleRequest 검증을 통과한다")
    void updateRequest_versionAndTitle_valid() throws Exception {
        UpdateScheduleRequest request = fromJson("""
                {"version": 1, "title": "변경된 제목"}
                """);

        Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    private UpdateScheduleRequest fromJson(String json) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return objectMapper.readValue(json, UpdateScheduleRequest.class);
    }
}