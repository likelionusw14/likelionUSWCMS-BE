package com.likelion.cms.service;

import com.likelion.cms.domain.Account;
import com.likelion.cms.domain.AccountStatus;
import com.likelion.cms.dto.AdditionalInfoRequest;
import com.likelion.cms.dto.SignupRequest;
import com.likelion.cms.dto.SignupResponse;
import com.likelion.cms.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        Account account = accountRepository.findByProviderAndProviderId(request.provider(), request.providerId())
                .orElseGet(() -> {
                    Account newAccount = Account.builder()
                            .provider(request.provider())
                            .providerId(request.providerId())
                            .accountStatus(AccountStatus.PENDING)
                            .build();
                    return accountRepository.save(newAccount);
                });

        return new SignupResponse(account.getId(), account.getAccountStatus().name(), null, null);
    }

    @Transactional
    public SignupResponse updateInfo(Long userId, AdditionalInfoRequest request) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        account.updateAdditionalInfo(request.name(), request.cohortId(), request.part());

        return new SignupResponse(
                account.getId(),
                account.getAccountStatus().name(),
                account.getName(),
                account.getPart()
        );
    }

    public Long findUserIdByProviderId(String provider, String providerId) {
        Account account = accountRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new IllegalArgumentException("가입된 유저를 찾을 수 없습니다."));
        return account.getId();
    }
}