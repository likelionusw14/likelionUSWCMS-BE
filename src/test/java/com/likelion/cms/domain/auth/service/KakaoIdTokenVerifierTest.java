package com.likelion.cms.domain.auth.service;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoIdTokenVerifierTest {

    @Mock
    private JwtDecoder kakaoIdTokenDecoder;

    private KakaoIdTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new KakaoIdTokenVerifier(kakaoIdTokenDecoder);
    }

    @Test
    void returnsSubjectWhenSignatureAndNonceAreValid() {
        Jwt jwt = jwtWithNonce("expected-nonce", "12345");
        when(kakaoIdTokenDecoder.decode("token")).thenReturn(jwt);

        String subject = verifier.verify("token", "expected-nonce");

        assertThat(subject).isEqualTo("12345");
    }

    @Test
    void rejectsWhenDecoderThrows() {
        when(kakaoIdTokenDecoder.decode("bad-token")).thenThrow(new JwtException("invalid"));

        assertThatThrownBy(() -> verifier.verify("bad-token", "expected-nonce"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void rejectsWhenNonceDoesNotMatch() {
        Jwt jwt = jwtWithNonce("other-nonce", "12345");
        when(kakaoIdTokenDecoder.decode("token")).thenReturn(jwt);

        assertThatThrownBy(() -> verifier.verify("token", "expected-nonce"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private Jwt jwtWithNonce(String nonce, String subject) {
        Instant issuedAt = Instant.now();
        return new Jwt("token", issuedAt, issuedAt.plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", subject, "nonce", nonce, "iss", "https://kauth.kakao.com"));
    }
}
