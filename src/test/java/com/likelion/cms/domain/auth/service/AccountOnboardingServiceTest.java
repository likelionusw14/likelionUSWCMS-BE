package com.likelion.cms.domain.auth.service;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.auth.dto.request.CreateAccountRequest;
import com.likelion.cms.domain.auth.dto.response.AccountResponse;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.entity.CohortStatus;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountOnboardingServiceTest {

    @Mock
    private OnboardingSessionStore onboardingSessionStore;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CohortRepository cohortRepository;

    private AccountOnboardingService service;

    private final CreateAccountRequest request =
            new CreateAccountRequest("홍길동", "컴퓨터공학과", "202012345", 1L, PartType.BACKEND);

    @BeforeEach
    void setUp() {
        service = new AccountOnboardingService(onboardingSessionStore, appUserRepository, cohortRepository);
    }

    @Test
    void rejectsMissingOnboardingSession() {
        assertThatThrownBy(() -> service.createAccount(null, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        verify(onboardingSessionStore, never()).resolve(any());
    }

    @Test
    void rejectsExpiredOrUnknownOnboardingSession() {
        when(onboardingSessionStore.resolve("session-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAccount("session-1", request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void rejectsDuplicateStudentId() {
        when(onboardingSessionStore.resolve("session-1")).thenReturn(Optional.of("kakao-sub-123"));
        when(appUserRepository.existsByStudentId("202012345")).thenReturn(true);

        assertThatThrownBy(() -> service.createAccount("session-1", request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(cohortRepository, never()).findById(any());
    }

    @Test
    void rejectsMissingCohort() {
        when(onboardingSessionStore.resolve("session-1")).thenReturn(Optional.of("kakao-sub-123"));
        when(appUserRepository.existsByStudentId("202012345")).thenReturn(false);
        when(cohortRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAccount("session-1", request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void fallsBackToConflictOnRaceConditionDuringSave() {
        when(onboardingSessionStore.resolve("session-1")).thenReturn(Optional.of("kakao-sub-123"));
        when(appUserRepository.existsByStudentId("202012345")).thenReturn(false);
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(testCohort()));
        when(appUserRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.createAccount("session-1", request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(onboardingSessionStore, never()).consume(any());
    }

    @Test
    void createsPendingMemberAccountAndConsumesSessionOnSuccess() {
        when(onboardingSessionStore.resolve("session-1")).thenReturn(Optional.of("kakao-sub-123"));
        when(appUserRepository.existsByStudentId("202012345")).thenReturn(false);
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(testCohort()));
        when(appUserRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            // Real JPA sets @Version/createdAt/updatedAt on INSERT; simulate that here
            // since the repository is mocked and never actually persists anything.
            AppUser user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "version", 0);
            ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.now());
            return user;
        });

        AccountResponse response = service.createAccount("session-1", request);

        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getStudentId()).isEqualTo("202012345");
        assertThat(response.getRole()).isEqualTo(SystemRole.MEMBER);
        assertThat(response.getStatus()).isEqualTo(AccountStatus.PENDING);
        verify(appUserRepository).saveAndFlush(any());
        verify(onboardingSessionStore).consume("session-1");
    }

    private Cohort testCohort() {
        return Cohort.builder()
                .number(14)
                .name("14기")
                .startedAt(LocalDate.now())
                .endedAt(LocalDate.now().plusMonths(6))
                .status(CohortStatus.ACTIVE)
                .build();
    }
}
