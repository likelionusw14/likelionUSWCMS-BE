package com.likelion.cms.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

/**
 * Provides a standalone {@link JwtDecoder} for verifying Kakao's OIDC
 * id_token against Kakao's JWKS. This is NOT the app's resource-server
 * config -- do not add spring.security.oauth2.resourceserver.* properties
 * or call .oauth2ResourceServer() anywhere, or Spring Boot will wire this
 * decoder into the main filter chain and try to authenticate our own
 * HS256 Bearer access tokens against Kakao's JWKS too.
 */
@Configuration
public class KakaoOidcConfig {

    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";
    private static final String KAKAO_JWK_SET_URI = "https://kauth.kakao.com/.well-known/jwks.json";

    @Bean
    public JwtDecoder kakaoIdTokenDecoder(@Value("${kakao.client-id}") String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(KAKAO_JWK_SET_URI).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(KAKAO_ISSUER);
        OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, audience -> audience != null && audience.contains(clientId));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }
}
