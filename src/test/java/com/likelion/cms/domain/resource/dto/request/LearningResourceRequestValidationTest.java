package com.likelion.cms.domain.resource.dto.request;

import com.likelion.cms.common.type.PartType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LearningResourceRequestValidationTest {

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
    void createRejectsWeekOutsideContractRange() {
        CreateLearningResourceRequest request = new CreateLearningResourceRequest(
                "자료", 53, PartType.BACKEND, 1L
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("week");
    }

    @Test
    void updateRequiresAtLeastOneChangeBesidesVersion() {
        UpdateLearningResourceRequest request = new UpdateLearningResourceRequest();
        request.setVersion(0);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("anyChangeProvided");
    }

    @Test
    void updateRejectsExplicitNullForNonNullableField() {
        UpdateLearningResourceRequest request = new UpdateLearningResourceRequest();
        request.setVersion(0);
        request.setFileAssetId(null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("providedValueValid");
    }
}
