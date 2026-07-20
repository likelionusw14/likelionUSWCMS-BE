package com.likelion.cms.domain.notice.dto.request;

import com.likelion.cms.domain.notice.entity.NoticeTag;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeRequestValidationTest {

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
    void createRequiresTag() {
        CreateNoticeRequest request = new CreateNoticeRequest(
                "공지", "내용", null, false, null, null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("tag");
    }

    @Test
    void createRejectsUnsafeExternalUrlScheme() {
        CreateNoticeRequest request = new CreateNoticeRequest(
                "공지", "내용", NoticeTag.OTHER, false, "javascript:alert(1)", null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("externalUrlValid");
    }

    @Test
    void updateRequiresAtLeastOneChangeBesidesVersion() {
        UpdateNoticeRequest request = new UpdateNoticeRequest();
        request.setVersion(0);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("anyChangeProvided");
    }

    @Test
    void updateAllowsExplicitNullForNullableFields() {
        UpdateNoticeRequest request = new UpdateNoticeRequest();
        request.setVersion(0);
        request.setExternalUrl(null);
        request.setImageAssetId(null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateRejectsExplicitNullForRequiredField() {
        UpdateNoticeRequest request = new UpdateNoticeRequest();
        request.setVersion(0);
        request.setTitle(null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("providedValueValid");
    }
}
