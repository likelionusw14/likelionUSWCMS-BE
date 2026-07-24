package com.likelion.cms.domain.auth.service;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieFactory {

    private static final String COOKIE_PATH = "/api";

    public static final String ONBOARDING_SESSION_COOKIE = "onboarding_session";
    public static final String REFRESH_SESSION_COOKIE = "refresh_session";
    public static final String CSRF_COOKIE = "csrf_token";

    private static final Duration ONBOARDING_SESSION_TTL = Duration.ofMinutes(20);
    private static final Duration REFRESH_SESSION_TTL = Duration.ofDays(14);

    public ResponseCookie onboardingSessionCookie(String sessionId) {
        return sessionCookie(ONBOARDING_SESSION_COOKIE, sessionId, ONBOARDING_SESSION_TTL, true);
    }

    public ResponseCookie refreshSessionCookie(String token) {
        return sessionCookie(REFRESH_SESSION_COOKIE, token, REFRESH_SESSION_TTL, true);
    }

    public ResponseCookie csrfCookieForOnboarding(String csrfToken) {
        return sessionCookie(CSRF_COOKIE, csrfToken, ONBOARDING_SESSION_TTL, false);
    }

    public ResponseCookie csrfCookieForRefresh(String csrfToken) {
        return sessionCookie(CSRF_COOKIE, csrfToken, REFRESH_SESSION_TTL, false);
    }

    public ResponseCookie clearOnboardingSessionCookie() {
        return sessionCookie(ONBOARDING_SESSION_COOKIE, "", Duration.ZERO, true);
    }

    public ResponseCookie clearRefreshSessionCookie() {
        return sessionCookie(REFRESH_SESSION_COOKIE, "", Duration.ZERO, true);
    }

    public ResponseCookie clearCsrfCookie() {
        return sessionCookie(CSRF_COOKIE, "", Duration.ZERO, false);
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
}
