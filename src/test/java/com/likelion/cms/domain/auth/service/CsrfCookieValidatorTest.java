package com.likelion.cms.domain.auth.service;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsrfCookieValidatorTest {

    private final CsrfCookieValidator validator = new CsrfCookieValidator();

    @Test
    void acceptsWhenCookieAndHeaderMatch() {
        assertThatCode(() -> validator.validate("token-1", "token-1")).doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenValuesDiffer() {
        assertThatThrownBy(() -> validator.validate("token-1", "different"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void rejectsWhenCookieMissing() {
        assertThatThrownBy(() -> validator.validate(null, "token-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void rejectsWhenHeaderMissing() {
        assertThatThrownBy(() -> validator.validate("token-1", null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void rejectsWhenBothValuesAreBlank() {
        assertThatThrownBy(() -> validator.validate("", ""))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void rejectsWhenCookieIsBlank() {
        assertThatThrownBy(() -> validator.validate("   ", "token-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
