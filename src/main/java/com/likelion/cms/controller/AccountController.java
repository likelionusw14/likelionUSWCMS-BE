package com.likelion.cms.controller;

import com.likelion.cms.global.jwt.JwtTokenProvider;
import com.likelion.cms.dto.*;
import com.likelion.cms.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        SignupResponse response = accountService.signup(request);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "가입 요청이 완료되었습니다.",
                "data", response
        ));
    }

    @PostMapping
    public ResponseEntity<?> updateAdditionalInfo(
            Authentication authentication,
            @RequestBody AdditionalInfoRequest request) {

        Long userId = (Long) authentication.getPrincipal();
        SignupResponse response = accountService.updateInfo(userId, request);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "추가 정보가 입력되었습니다.",
                "data", response
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody SignupRequest request) {
        Long userId = accountService.login(request);

        String token = jwtTokenProvider.createToken(userId);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "로그인 성공 (토큰 발급)",
                "token", token
        ));
    }
}