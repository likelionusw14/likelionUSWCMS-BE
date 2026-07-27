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

import java.net.URI;
import java.util.List;
import java.util.Optional;

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

    @Value("${app.frontend.onboarding-redirect-uri}")
    private String onboardingRedirectUri;

    @Value("${app.frontend.app-redirect-uri}")
    private String appRedirectUri;

    @Value("${app.frontend.error-redirect-uri}")
    private String errorRedirectUri;

    @Value("${jwt.access-token-validity-seconds}")
    private long accessTokenValiditySeconds;

    public String startKakaoLogin() {
        String state = OpaqueTokenGenerator.generate();
        String nonce = OpaqueTokenGenerator.generate();
        oidcStateStore.save(state, nonce);
        return kakaoOidcClient.buildAuthorizeUrl(state, nonce);
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
        if (StringUtils.hasText(error) || !StringUtils.hasText(code)) {
            return new CallbackResult(URI.create(errorRedirectUri), List.of());
        }

        try {
            return doHandleKakaoCallback(code, state);
        } catch (BusinessException e) {
            log.warn("카카오 로그인 콜백 처리 실패: {}", e.getMessage());
            return new CallbackResult(URI.create(errorRedirectUri), List.of());
        }
    }

    private CallbackResult doHandleKakaoCallback(String code, String state) {
        String nonce = oidcStateStore.consumeNonce(state)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "로그인 요청이 만료되었거나 유효하지 않습니다."));

        KakaoTokenResponse tokenResponse = kakaoOidcClient.exchangeCodeForTokens(code);
        String kakaoSubject = kakaoIdTokenVerifier.verify(tokenResponse.idToken(), nonce);

        Optional<AppUser> existingUser = appUserRepository.findByKakaoSubject(kakaoSubject);
        String csrfToken = OpaqueTokenGenerator.generate();

        if (existingUser.isPresent()) {
            String refreshToken = refreshTokenStore.issue(existingUser.get().getUserId());
            List<ResponseCookie> cookies = List.of(
                    authCookieFactory.refreshSessionCookie(refreshToken),
                    authCookieFactory.csrfCookieForRefresh(csrfToken));
            return new CallbackResult(URI.create(appRedirectUri), cookies);
        }

        String onboardingSessionId = onboardingSessionStore.create(kakaoSubject);
        List<ResponseCookie> cookies = List.of(
                authCookieFactory.onboardingSessionCookie(onboardingSessionId),
                authCookieFactory.csrfCookieForOnboarding(csrfToken));
        return new CallbackResult(URI.create(onboardingRedirectUri), cookies);
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
