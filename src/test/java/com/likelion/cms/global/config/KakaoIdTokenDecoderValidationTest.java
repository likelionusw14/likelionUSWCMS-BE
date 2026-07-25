package com.likelion.cms.global.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the same validator composition used in {@link KakaoOidcConfig}
 * (issuer + audience, on top of NimbusJwtDecoder's built-in signature/exp
 * checks) actually accepts/rejects the tokens it should, using a locally
 * signed test JWT instead of hitting Kakao's real JWKS endpoint.
 */
class KakaoIdTokenDecoderValidationTest {

    private static final String ISSUER = "https://kauth.kakao.com";
    private static final String CLIENT_ID = "test-client-id";

    private static RSAPublicKey publicKey;
    private static RSAPrivateKey privateKey;
    private static JwtDecoder decoder;

    @BeforeAll
    static void setUpKeysAndDecoder() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(ISSUER);
        OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, audience -> audience != null && audience.contains(CLIENT_ID));
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        decoder = nimbusDecoder;
    }

    @Test
    void acceptsValidToken() {
        String token = sign(ISSUER, CLIENT_ID, Instant.now().plusSeconds(3600), privateKey);

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("kakao-sub-123");
    }

    @Test
    void rejectsWrongIssuer() {
        String token = sign("https://not-kakao.example.com", CLIENT_ID, Instant.now().plusSeconds(3600), privateKey);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongAudience() {
        String token = sign(ISSUER, "someone-elses-client-id", Instant.now().plusSeconds(3600), privateKey);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String token = sign(ISSUER, CLIENT_ID, Instant.now().minusSeconds(60), privateKey);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsBadSignature() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPrivateKey wrongPrivateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();

        String token = sign(ISSUER, CLIENT_ID, Instant.now().plusSeconds(3600), wrongPrivateKey);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private String sign(String issuer, String audience, Instant expiry, RSAPrivateKey signingKey) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audience)
                    .subject("kakao-sub-123")
                    .claim("nonce", "test-nonce")
                    .issueTime(Date.from(Instant.now().minusSeconds(10)))
                    .expirationTime(Date.from(expiry))
                    .build();

            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            signedJwt.sign(new RSASSASigner(signingKey));
            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
