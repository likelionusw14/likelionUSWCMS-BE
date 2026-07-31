package com.likelion.cms.domain.auth.service;

import com.likelion.cms.domain.auth.dto.response.AccessTokenResponse;
import com.likelion.cms.domain.auth.dto.response.AccountResponse;
import com.likelion.cms.domain.auth.dto.response.KakaoTokenResponse;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final OidcStateStore oidcStateStore;
    private final RefreshTokenStore refreshTokenStore;
    private final OnboardingSessionStore onboardingSessionStore;
    private final KakaoOidcClient kakaoOidcClient;
    private final KakaoIdTokenVerifier kakaoIdTokenVerifier;
    private final AuthCookieFactory authCookieFactory;
    private final AppUserRepository appUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend.default-origin}")
    private String defaultFrontendOrigin;

    @Value("${app.frontend.callback-path}")
    private String callbackPath;

    @Value("${app.frontend.allowed-origins:}")
    private String allowedOriginsRaw;

    @Value("${jwt.access-token-validity-seconds}")
    private long accessTokenValiditySeconds;

    /**
     * requestedOrigin lets a client (e.g. a local dev frontend) ask to be
     * redirected back to itself after login instead of the configured
     * default. Only exact matches against app.frontend.allowed-origins are
     * honored -- anything else is silently dropped (falls back to the
     * default redirect URI) rather than rejecting the login, since this is
     * a convenience feature, not something the request should fail over.
     */
    public String startKakaoLogin(String requestedOrigin) {
        String state = OpaqueTokenGenerator.generate();
        String nonce = OpaqueTokenGenerator.generate();
        String validatedOrigin = allowedOrigins().contains(requestedOrigin) ? requestedOrigin : null;
        oidcStateStore.save(state, nonce, validatedOrigin);
        return kakaoOidcClient.buildAuthorizeUrl(state, nonce);
    }

    private Set<String> allowedOrigins() {
        if (!StringUtils.hasText(allowedOriginsRaw)) {
            return Set.of();
        }
        return Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    /**
     * NOT_SUPPORTED: this method makes a blocking outbound HTTP call to Kakao
     * (exchangeCodeForTokens) plus Redis operations, with only a single simple
     * JPA read in between. Letting the class-level transaction wrap the whole
     * method would hold a pooled DB connection for the full duration of that
     * external call; the JPA read still gets its own short transaction from
     * Spring Data's repository proxy when called outside of one.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CallbackResult handleKakaoCallback(String code, String state, String error) {
        OidcStateStore.StateValue stateValue = StringUtils.hasText(state)
                ? oidcStateStore.consume(state).orElse(null)
                : null;
        String origin = stateValue == null ? null : stateValue.frontendOrigin();

        if (StringUtils.hasText(error)) {
            return errorResult(origin, "access_denied");
        }
        if (stateValue == null) {
            return errorResult(origin, "invalid_state");
        }
        if (!StringUtils.hasText(code)) {
            return errorResult(origin, "server_error");
        }

        try {
            return doHandleKakaoCallback(code, stateValue, origin);
        } catch (KakaoExchangeFailedException e) {
            log.warn("카카오 토큰 교환 실패: {}", e.getCause().getMessage());
            return errorResult(origin, "kakao_error");
        } catch (BusinessException e) {
            log.warn("카카오 로그인 콜백 처리 실패: {}", e.getMessage());
            return errorResult(origin, "server_error");
        }
    }

    private CallbackResult doHandleKakaoCallback(String code, OidcStateStore.StateValue stateValue, String origin) {
        KakaoTokenResponse tokenResponse;
        try {
            tokenResponse = kakaoOidcClient.exchangeCodeForTokens(code);
        } catch (BusinessException e) {
            throw new KakaoExchangeFailedException(e);
        }
        String kakaoSubject = kakaoIdTokenVerifier.verify(tokenResponse.idToken(), stateValue.nonce());

        Optional<AppUser> existingUser = appUserRepository.findByKakaoSubject(kakaoSubject);
        String csrfToken = OpaqueTokenGenerator.generate();

        if (existingUser.isPresent()) {
            String refreshToken = refreshTokenStore.issue(existingUser.get().getUserId());
            List<ResponseCookie> cookies = List.of(
                    authCookieFactory.refreshSessionCookie(refreshToken),
                    authCookieFactory.csrfCookieForRefresh(csrfToken),
                    authCookieFactory.legacyCsrfCookieCleared());
            return new CallbackResult(callbackUri(origin, "status", "member"), cookies);
        }

        String onboardingSessionId = onboardingSessionStore.create(kakaoSubject);
        List<ResponseCookie> cookies = List.of(
                authCookieFactory.onboardingSessionCookie(onboardingSessionId),
                authCookieFactory.csrfCookieForOnboarding(csrfToken),
                authCookieFactory.legacyCsrfCookieCleared());
        return new CallbackResult(callbackUri(origin, "status", "onboarding"), cookies);
    }

    private CallbackResult errorResult(String origin, String errorCode) {
        return new CallbackResult(callbackUri(origin, "error", errorCode), List.of());
    }

    /**
     * Builds {origin}{callbackPath}?paramName=paramValue, using the caller's
     * validated origin when present (see startKakaoLogin) and falling back
     * to app.frontend.default-origin otherwise.
     */
    private URI callbackUri(String validatedOrigin, String paramName, String paramValue) {
        String base = (validatedOrigin != null ? validatedOrigin : defaultFrontendOrigin) + callbackPath;
        return UriComponentsBuilder.fromUriString(base)
                .queryParam(paramName, paramValue)
                .build()
                .toUri();
    }

    private static final class KakaoExchangeFailedException extends RuntimeException {
        KakaoExchangeFailedException(Throwable cause) {
            super(cause);
        }
    }

    public ReissueResult reissueAccessToken(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = refreshTokenStore.consume(rawRefreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."));
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        String newRefreshToken = refreshTokenStore.issue(userId);
        String newCsrfToken = OpaqueTokenGenerator.generate();
        String accessToken = jwtTokenProvider.createToken(userId, user.getSystemRole());

        AccessTokenResponse body = AccessTokenResponse.of(
                accessToken, "Bearer", accessTokenValiditySeconds, AccountResponse.from(user));

        return new ReissueResult(body,
                authCookieFactory.refreshSessionCookie(newRefreshToken),
                authCookieFactory.csrfCookieForRefresh(newCsrfToken));
    }

    public void revokeSession(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        refreshTokenStore.revoke(rawRefreshToken);
    }

    public record CallbackResult(URI redirectUri, List<ResponseCookie> cookies) {
    }

    public record ReissueResult(AccessTokenResponse body, ResponseCookie refreshCookie, ResponseCookie csrfCookie) {
    }
}
