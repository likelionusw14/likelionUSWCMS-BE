package com.likelion.cms.domain.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieFactoryTest {

    private final AuthCookieFactory factory = new AuthCookieFactory();

    @Test
    void csrfCookieHasNoDomainWhenNotConfigured() {
        ResponseCookie cookie = factory.csrfCookieForOnboarding("token-1");

        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isFalse();
    }

    @Test
    void csrfCookieUsesConfiguredDomainForCrossSubdomainAccess() {
        ReflectionTestUtils.setField(factory, "csrfCookieDomain", "usw-likelion.kr");

        ResponseCookie cookie = factory.csrfCookieForRefresh("token-2");

        assertThat(cookie.getDomain()).isEqualTo("usw-likelion.kr");
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void sessionCookiesStayHostOnlyAndScopedToApiPath() {
        ReflectionTestUtils.setField(factory, "csrfCookieDomain", "usw-likelion.kr");

        ResponseCookie onboarding = factory.onboardingSessionCookie("session-1");
        ResponseCookie refresh = factory.refreshSessionCookie("refresh-1");

        assertThat(onboarding.getDomain()).isNull();
        assertThat(onboarding.getPath()).isEqualTo("/api");
        assertThat(onboarding.isHttpOnly()).isTrue();
        assertThat(refresh.getDomain()).isNull();
        assertThat(refresh.getPath()).isEqualTo("/api");
        assertThat(refresh.isHttpOnly()).isTrue();
    }

    @Test
    void legacyCsrfCookieClearedMatchesThePreDomainFixScopeExactly() {
        ReflectionTestUtils.setField(factory, "csrfCookieDomain", "usw-likelion.kr");

        ResponseCookie legacy = factory.legacyCsrfCookieCleared();

        // Must match the old Set-Cookie's Domain+Path exactly (host-only,
        // /api) or the browser won't recognize it as the same cookie to expire.
        assertThat(legacy.getDomain()).isNull();
        assertThat(legacy.getPath()).isEqualTo("/api");
        assertThat(legacy.getMaxAge()).isZero();
    }
}
