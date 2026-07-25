package com.likelion.cms.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessTokenResponse {
    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final AccountResponse account;

    public static AccessTokenResponse of(String accessToken, String tokenType, long expiresIn, AccountResponse account) {
        return new AccessTokenResponse(accessToken, tokenType, expiresIn, account);
    }
}
