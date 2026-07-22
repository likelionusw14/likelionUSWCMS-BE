package com.likelion.cms.domain.user.service;

import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.user.dto.request.ApproveAccountRequest;
import com.likelion.cms.domain.user.dto.request.RejectAccountRequest;
import com.likelion.cms.domain.user.dto.request.UpdateAccountRequest;
import com.likelion.cms.domain.user.dto.request.UpdateRoleRequest;
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
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.PENDING, SystemRole.MEMBER, 0);
        ApproveAccountRequest request = new ApproveAccountRequest(0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        AccountResponse response = userService.approve(2L, request, 1L);

        assertThat(response.userId()).isEqualTo(2L);
        verify(target).approve(actor);
    }

    @Test
    void rejectSetsReasonOnPendingAccount() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.PENDING, SystemRole.MEMBER, 0);
        RejectAccountRequest request = new RejectAccountRequest(" 서류 미비 ", 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.reject(2L, request, 1L);

        verify(target).reject(actor, "서류 미비");
    }

    @Test
    void approveRejectsAlreadyDecidedAccount() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, SystemRole.MEMBER, 0);
        ApproveAccountRequest request = new ApproveAccountRequest(0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.approve(2L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(target, never()).approve(any());
    }

    @Test
    void changeRoleRejectsNonActiveAccount() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.PENDING, SystemRole.MEMBER, 0);
        UpdateRoleRequest request = new UpdateRoleRequest(SystemRole.ADMIN, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.changeRole(2L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(target, never()).changeRole(any());
    }

    @Test
    void changeRoleRejectsDemotingLastAdmin() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        UpdateRoleRequest request = new UpdateRoleRequest(SystemRole.MEMBER, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));
        when(appUserRepository.countBySystemRole(SystemRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.changeRole(2L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(target, never()).changeRole(any());
    }

    @Test
    void changeRoleAllowsDemotingWhenOtherAdminsExist() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        UpdateRoleRequest request = new UpdateRoleRequest(SystemRole.MEMBER, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));
        when(appUserRepository.countBySystemRole(SystemRole.ADMIN)).thenReturn(2L);

        userService.changeRole(2L, request, 1L);

        verify(target).changeRole(SystemRole.MEMBER);
    }

    @Test
    void updateAppliesOnlyProvidedFieldsIncludingStudentId() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, SystemRole.MEMBER, 0);
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setVersion(0);
        request.setStudentId(" 2021000000 ");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.update(2L, request, 1L);

        verify(target).updateStudentId("2021000000");
        verify(target, never()).updateName(anyString());
    }

    @Test
    void deleteRejectsDeletingLastAdmin() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));
        when(appUserRepository.countBySystemRole(SystemRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.delete(2L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(target, never()).delete();
    }

    @Test
    void deleteSoftDeletesNonLastAdmin() {
        AppUser actor = accountWith(1L, AccountStatus.ACTIVE, SystemRole.ADMIN, 0);
        AppUser target = accountWith(2L, AccountStatus.ACTIVE, SystemRole.MEMBER, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.delete(2L, 1L);

        verify(target).delete();
    }

    private AppUser accountWith(Long userId, AccountStatus accountStatus, SystemRole systemRole, Integer version) {
        AppUser account = mock(AppUser.class);
        lenient().when(account.getUserId()).thenReturn(userId);
        lenient().when(account.getAccountStatus()).thenReturn(accountStatus);
        lenient().when(account.getSystemRole()).thenReturn(systemRole);
        lenient().when(account.getVersion()).thenReturn(version);
        lenient().when(account.getName()).thenReturn("기존 이름");
        lenient().when(account.getDepartment()).thenReturn("기존 학과");
        lenient().when(account.getStudentId()).thenReturn("2021000000");
        lenient().when(account.getCohort()).thenReturn(mock(Cohort.class));
        return account;
    }
}
