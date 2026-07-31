package com.likelion.cms.domain.auth.controller;

import com.likelion.cms.domain.auth.dto.response.AccessTokenResponse;
import com.likelion.cms.domain.auth.service.AuthCookieFactory;
import com.likelion.cms.domain.auth.service.AuthService;
import com.likelion.cms.domain.auth.service.CsrfCookieValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String CSRF_HEADER = "X-CSRF-Token";

    private final AuthService authService;
    private final AuthCookieFactory authCookieFactory;
    private final CsrfCookieValidator csrfCookieValidator;

    @GetMapping("/kakao/login")
    public ResponseEntity<Void> loginRedirect(
            @RequestParam(required = false) String origin) {
        String authorizeUrl = authService.startKakaoLogin(origin);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authorizeUrl)).build();
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        AuthService.CallbackResult result = authService.handleKakaoCallback(code, state, error);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.FOUND).location(result.redirectUri());
        result.cookies().forEach(cookie -> builder.header(HttpHeaders.SET_COOKIE, cookie.toString()));
        return builder.build();
    }

    @PostMapping("/tokens")
    public ResponseEntity<AccessTokenResponse> reissueAccessToken(
            @CookieValue(name = AuthCookieFactory.REFRESH_SESSION_COOKIE, required = false) String refreshToken,
            @CookieValue(name = AuthCookieFactory.CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = CSRF_HEADER, required = false) String csrfHeader) {
        csrfCookieValidator.validate(csrfCookie, csrfHeader);
        AuthService.ReissueResult result = authService.reissueAccessToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.csrfCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieFactory.legacyCsrfCookieCleared().toString())
                .body(result.body());
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieFactory.REFRESH_SESSION_COOKIE, required = false) String refreshToken,
            @CookieValue(name = AuthCookieFactory.CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = CSRF_HEADER, required = false) String csrfHeader) {
        csrfCookieValidator.validate(csrfCookie, csrfHeader);
        authService.revokeSession(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieFactory.clearRefreshSessionCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieFactory.clearCsrfCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieFactory.legacyCsrfCookieCleared().toString())
                .build();
    }
}
