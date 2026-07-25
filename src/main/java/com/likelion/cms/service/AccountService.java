package com.likelion.cms.service;

import com.likelion.cms.domain.Account;
import com.likelion.cms.domain.AccountStatus;
import com.likelion.cms.dto.AdditionalInfoRequest;
import com.likelion.cms.dto.SignupRequest;
import com.likelion.cms.dto.SignupResponse;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private static final String KAKAO_PROVIDER = "kakao";

    private final AccountRepository accountRepository;
    private final KakaoAuthClient kakaoAuthClient;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String providerId = resolveProviderId(request.provider(), request.accessToken());

        Account account = accountRepository.findByProviderAndProviderId(request.provider(), providerId)
                .orElseGet(() -> {
                    Account newAccount = Account.builder()
                            .provider(request.provider())
                            .providerId(providerId)
                            .accountStatus(AccountStatus.PENDING)
                            .build();
                    return accountRepository.save(newAccount);
                });

        return new SignupResponse(account.getId(), account.getAccountStatus().name(), account.getName(), account.getPart());
    }

    @Transactional
    public SignupResponse updateInfo(Long userId, AdditionalInfoRequest request) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 유저를 찾을 수 없습니다."));

        account.updateAdditionalInfo(request.name(), request.cohortId(), request.part());

        return new SignupResponse(
                account.getId(),
                account.getAccountStatus().name(),
                account.getName(),
                account.getPart()
        );
    }

    public Long login(SignupRequest request) {
        String providerId = resolveProviderId(request.provider(), request.accessToken());

        Account account = accountRepository.findByProviderAndProviderId(request.provider(), providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "가입된 유저를 찾을 수 없습니다."));

        return account.getId();
    }

    private String resolveProviderId(String provider, String accessToken) {
        if (!KAKAO_PROVIDER.equalsIgnoreCase(provider)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 provider입니다.");
        }
        return kakaoAuthClient.getUserId(accessToken);
    }
}