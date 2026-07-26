package com.likelion.cms;

import com.likelion.cms.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TempTestController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/test/token")
    public String getTestToken() {
        return jwtTokenProvider.createToken(1L); // userId=1 (홍길동)
    }
}