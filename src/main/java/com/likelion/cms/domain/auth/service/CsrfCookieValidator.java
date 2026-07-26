package com.likelion.cms.domain.auth.service;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Double-submit CSRF check: the frontend reads the non-HttpOnly csrf_token
 * cookie via JS and echoes it back as the X-CSRF-Token header. No
 * server-side storage is needed -- correctness only depends on the header
 * matching the cookie the browser is sending back.
 */
@Component
public class CsrfCookieValidator {

    public void validate(String csrfCookieValue, String csrfHeaderValue) {
        if (!StringUtils.hasText(csrfCookieValue) || !StringUtils.hasText(csrfHeaderValue)
                || !MessageDigest.isEqual(
                        csrfCookieValue.getBytes(StandardCharsets.UTF_8),
                        csrfHeaderValue.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "CSRF 토큰이 유효하지 않습니다.");
        }
    }
}
