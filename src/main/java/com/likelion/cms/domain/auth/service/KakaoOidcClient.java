package com.likelion.cms.domain.auth.service;

import com.likelion.cms.domain.auth.dto.response.KakaoTokenResponse;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KakaoOidcClient {

    private static final String AUTHORIZE_URI = "https://kauth.kakao.com/oauth/authorize";
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 5000;

    private final RestClient restClient;

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoOidcClient(
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.client-secret:}") String clientSecret,
            @Value("${kakao.redirect-uri}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
        this.restClient = RestClient.builder()
                .baseUrl("https://kauth.kakao.com")
                .requestFactory(requestFactory)
                .build();
    }

    public String buildAuthorizeUrl(String state, String nonce) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URI)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "openid")
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .build()
                .toUriString();
    }

    public KakaoTokenResponse exchangeCodeForTokens(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (StringUtils.hasText(clientSecret)) {
            form.add("client_secret", clientSecret);
        }

        KakaoTokenResponse tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 인증 코드 교환에 실패했습니다.");
        }

        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.idToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오로부터 유효한 토큰을 받지 못했습니다.");
        }
        return tokenResponse;
    }
}
