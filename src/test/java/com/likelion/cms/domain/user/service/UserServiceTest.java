package com.likelion.cms.domain.user.service;

import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.user.dto.request.ChangeAccountRoleRequest;
import com.likelion.cms.domain.user.dto.request.RejectAccountRequest;
import com.likelion.cms.domain.user.dto.request.UpdateAccountRequest;
import com.likelion.cms.domain.user.dto.response.AccountResponse;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CohortRepository cohortRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(appUserRepository, cohortRepository);
    }

    @Test
    void approveActivatesPendingAccount() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, 0);
        AppUser target = accountWith(2L, AccountStatus.PENDING, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        AccountResponse response = userService.approve(2L, 1L);

        assertThat(response.userId()).isEqualTo(2L);
        verify(target).approve(actor);
    }

    @Test
    void approveRejectsAlreadyDecidedAccount() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.approve(2L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(target, never()).approve(actor);
    }

    @Test
    void rejectSetsReasonOnPendingAccount() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, 0);
        AppUser target = accountWith(2L, AccountStatus.PENDING, 0);
        RejectAccountRequest request = new RejectAccountRequest(" 서류 미비 ");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.reject(2L, request, 1L);

        verify(target).reject(actor, "서류 미비");
    }

    @Test
    void changeRoleUpdatesRoleOnVersionMatch() {
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, 1);
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, 0);
        ChangeAccountRoleRequest request = new ChangeAccountRoleRequest(SystemRole.ADMIN, 1);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.changeRole(2L, request, 1L);

        verify(target).changeRole(SystemRole.ADMIN);
    }

    @Test
    void changeRoleRejectsVersionMismatch() {
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, 1);
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, 0);
        ChangeAccountRoleRequest request = new ChangeAccountRoleRequest(SystemRole.ADMIN, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.changeRole(2L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTIMISTIC_LOCK_CONFLICT));
        verify(target, never()).changeRole(SystemRole.ADMIN);
    }

    @Test
    void changeRoleRejectsSelfDemotion() {
        AppUser self = accountWith(1L, AccountStatus.ACTIVE, 0);
        ChangeAccountRoleRequest request = new ChangeAccountRoleRequest(SystemRole.MEMBER, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> userService.changeRole(1L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(self, never()).changeRole(SystemRole.MEMBER);
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, 0);
        Cohort cohort = mock(Cohort.class);
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setVersion(0);
        request.setName(" 홍길동 ");
        request.setCohortId(5L);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));

        userService.update(2L, request, 1L);

        verify(target).updateName("홍길동");
        verify(target).updateCohort(cohort);
        verify(target, never()).updateDepartment(anyString());
    }

    @Test
    void updateRejectsMissingCohort() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, 0);
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setVersion(0);
        request.setCohortId(5L);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));
        when(cohortRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(2L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(target, never()).updateCohort(any());
    }

    @Test
    void deleteRejectsSelfDelete() {
        AppUser self = accountWith(1L, AccountStatus.ACTIVE, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> userService.delete(1L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(self, never()).delete();
    }

    @Test
    void deleteSoftDeletesTarget() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.delete(2L, 1L);

        verify(target).delete();
    }

    private AppUser accountWith(Long userId, AccountStatus accountStatus, Integer version) {
        AppUser account = mock(AppUser.class);
        lenient().when(account.getUserId()).thenReturn(userId);
        lenient().when(account.getAccountStatus()).thenReturn(accountStatus);
        lenient().when(account.getVersion()).thenReturn(version);
        lenient().when(account.getName()).thenReturn("기존 이름");
        lenient().when(account.getDepartment()).thenReturn("기존 학과");
        lenient().when(account.getStudentId()).thenReturn("2021000000");
        lenient().when(account.getCohort()).thenReturn(mock(Cohort.class));
        return account;
    }
}
