package com.likelion.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(Long id) {}
