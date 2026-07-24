package com.likelion.cms.domain.auth.service;

import com.likelion.cms.domain.auth.dto.request.CreateAccountRequest;
import com.likelion.cms.domain.auth.dto.response.AccountResponse;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountOnboardingService {

    private final OnboardingSessionStore onboardingSessionStore;
    private final AppUserRepository appUserRepository;
    private final CohortRepository cohortRepository;

    @Transactional
    public AccountResponse createAccount(String onboardingSessionId, CreateAccountRequest request) {
        if (!StringUtils.hasText(onboardingSessionId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String kakaoSubject = onboardingSessionStore.resolve(onboardingSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "온보딩 세션이 유효하지 않거나 만료되었습니다."));

        if (appUserRepository.existsByStudentId(request.studentId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 학번입니다.");
        }

        Cohort cohort = cohortRepository.findById(request.cohortId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 기수입니다."));

        AppUser user = AppUser.builder()
                .kakaoSubject(kakaoSubject)
                .name(request.name())
                .department(request.department())
                .studentId(request.studentId())
                .cohort(cohort)
                .part(request.part())
                .build();

        try {
            user = appUserRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 학번입니다.");
        }

        onboardingSessionStore.consume(onboardingSessionId);
        return AccountResponse.from(user);
    }
}
