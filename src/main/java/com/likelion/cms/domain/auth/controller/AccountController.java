package com.likelion.cms.domain.auth.controller;

import com.likelion.cms.domain.auth.dto.request.CreateAccountRequest;
import com.likelion.cms.domain.auth.dto.response.AccountResponse;
import com.likelion.cms.domain.auth.service.AccountOnboardingService;
import com.likelion.cms.domain.auth.service.AuthCookieFactory;
import com.likelion.cms.domain.auth.service.CsrfCookieValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final String CSRF_HEADER = "X-CSRF-Token";

    private final AccountOnboardingService accountOnboardingService;
    private final CsrfCookieValidator csrfCookieValidator;

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @CookieValue(name = AuthCookieFactory.ONBOARDING_SESSION_COOKIE, required = false) String onboardingSessionId,
            @CookieValue(name = AuthCookieFactory.CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = CSRF_HEADER, required = false) String csrfHeader,
            @Valid @RequestBody CreateAccountRequest request) {
        csrfCookieValidator.validate(csrfCookie, csrfHeader);
        AccountResponse response = accountOnboardingService.createAccount(onboardingSessionId, request);
        return ResponseEntity.created(URI.create("/api/accounts/" + response.getUserId())).body(response);
    }
}
