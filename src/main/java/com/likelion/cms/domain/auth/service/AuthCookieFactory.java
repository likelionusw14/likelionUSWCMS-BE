package com.likelion.cms.domain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class AuthCookieFactory {

    private static final String COOKIE_PATH = "/api";
    private static final String CSRF_COOKIE_PATH = "/";

    public static final String ONBOARDING_SESSION_COOKIE = "onboarding_session";
    public static final String REFRESH_SESSION_COOKIE = "refresh_session";
    public static final String CSRF_COOKIE = "csrf_token";

    private static final Duration ONBOARDING_SESSION_TTL = Duration.ofMinutes(20);
    private static final Duration REFRESH_SESSION_TTL = Duration.ofDays(14);

    /**
     * Blank for local/dev origins (localhost can't take a Domain attribute
     * anyway). In production this is the shared parent domain (e.g.
     * usw-likelion.kr) so the frontend's JS -- served from a different
     * subdomain than the API -- can read this non-HttpOnly cookie via
     * document.cookie to echo it back as the CSRF header. The two session
     * cookies stay HttpOnly + host-only + /api: only the browser needs
     * them, so there's no reason to widen their scope.
     */
    @Value("${app.cookie.csrf-domain:}")
    private String csrfCookieDomain;

    public ResponseCookie onboardingSessionCookie(String sessionId) {
        return sessionCookie(ONBOARDING_SESSION_COOKIE, sessionId, ONBOARDING_SESSION_TTL, true);
    }

    public ResponseCookie refreshSessionCookie(String token) {
        return sessionCookie(REFRESH_SESSION_COOKIE, token, REFRESH_SESSION_TTL, true);
    }

    public ResponseCookie csrfCookieForOnboarding(String csrfToken) {
        return csrfCookie(csrfToken, ONBOARDING_SESSION_TTL);
    }

    public ResponseCookie csrfCookieForRefresh(String csrfToken) {
        return csrfCookie(csrfToken, REFRESH_SESSION_TTL);
    }

    public ResponseCookie clearOnboardingSessionCookie() {
        return sessionCookie(ONBOARDING_SESSION_COOKIE, "", Duration.ZERO, true);
    }

    public ResponseCookie clearRefreshSessionCookie() {
        return sessionCookie(REFRESH_SESSION_COOKIE, "", Duration.ZERO, true);
    }

    public ResponseCookie clearCsrfCookie() {
        return csrfCookie("", Duration.ZERO);
    }

    private ResponseCookie sessionCookie(String name, String value, Duration maxAge, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie csrfCookie(String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(CSRF_COOKIE, value)
                .httpOnly(false)
                .secure(true)
                .sameSite("Lax")
                .path(CSRF_COOKIE_PATH)
                .maxAge(maxAge);
        if (StringUtils.hasText(csrfCookieDomain)) {
            builder.domain(csrfCookieDomain);
        }
        return builder.build();
    }
}
