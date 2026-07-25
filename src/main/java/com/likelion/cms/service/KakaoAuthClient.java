package com.likelion.cms.service;

import com.likelion.cms.dto.KakaoUserInfoResponse;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoAuthClient {

    private final RestClient restClient = RestClient.create("https://kapi.kakao.com");

    public String getUserId(String accessToken) {
        KakaoUserInfoResponse response;
        try {
            response = restClient.get()
                    .uri("/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 카카오 액세스 토큰입니다.");
        }

        if (response == null || response.id() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 사용자 정보를 확인할 수 없습니다.");
        }
        return String.valueOf(response.id());
    }
}
