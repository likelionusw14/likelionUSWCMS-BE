package com.likelion.cms.domain.auth.service;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class KakaoIdTokenVerifier {

    private final JwtDecoder kakaoIdTokenDecoder;

    /**
     * Verifies signature, iss, aud, exp (via the decoder's validator) and
     * the nonce claim (Spring has no built-in nonce validator, so this is
     * checked manually), then returns the Kakao subject (sub claim).
     */
    public String verify(String idToken, String expectedNonce) {
        Jwt jwt;
        try {
            jwt = kakaoIdTokenDecoder.decode(idToken);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 카카오 ID 토큰입니다.");
        }

        String nonce = jwt.getClaimAsString("nonce");
        if (!Objects.equals(nonce, expectedNonce)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 ID 토큰의 nonce가 일치하지 않습니다.");
        }

        return jwt.getSubject();
    }
}
