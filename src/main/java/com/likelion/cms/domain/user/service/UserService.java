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
import com.likelion.cms.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository appUserRepository;
    private final CohortRepository cohortRepository;

    public PageResponse<AccountResponse> list(AccountStatus accountStatus, Pageable pageable) {
        Page<AppUser> accounts = accountStatus == null
                ? appUserRepository.findAll(pageable)
                : appUserRepository.findByAccountStatus(accountStatus, pageable);
        return PageResponse.from(accounts.map(AccountResponse::from));
    }

    @Transactional
    public AccountResponse approve(Long userId, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        AppUser target = findAccount(userId);
        validatePendingStatus(target);
        target.approve(actor);
        return AccountResponse.from(target);
    }

    @Transactional
    public AccountResponse reject(Long userId, RejectAccountRequest request, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        AppUser target = findAccount(userId);
        validatePendingStatus(target);
        target.reject(actor, request.rejectionReason().trim());
        return AccountResponse.from(target);
    }

    @Transactional
    public AccountResponse changeRole(Long userId, ChangeAccountRoleRequest request, Long actorUserId) {
        findActor(actorUserId);
        AppUser target = findAccount(userId);
        validateVersion(target.getVersion(), request.version());
        if (target.getUserId().equals(actorUserId) && request.systemRole() != SystemRole.ADMIN) {
            throw new BusinessException(ErrorCode.CONFLICT, "자기 자신의 관리자 권한은 해제할 수 없습니다.");
        }
        target.changeRole(request.systemRole());
        return AccountResponse.from(target);
    }

    @Transactional
    public AccountResponse update(Long userId, UpdateAccountRequest request, Long actorUserId) {
        findActor(actorUserId);
        AppUser target = findAccount(userId);
        validateVersion(target.getVersion(), request.getVersion());

        if (request.isNameProvided()) {
            target.updateName(request.getName().trim());
        }
        if (request.isDepartmentProvided()) {
            target.updateDepartment(request.getDepartment().trim());
        }
        if (request.isPartProvided()) {
            target.updatePart(request.getPart());
        }
        if (request.isCohortIdProvided()) {
            target.updateCohort(findCohort(request.getCohortId()));
        }

        return AccountResponse.from(target);
    }

    @Transactional
    public void delete(Long userId, Long actorUserId) {
        findActor(actorUserId);
        AppUser target = findAccount(userId);
        if (target.getUserId().equals(actorUserId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "자기 자신은 삭제할 수 없습니다.");
        }
        target.delete();
    }

    private AppUser findActor(Long actorUserId) {
        return appUserRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private AppUser findAccount(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Cohort findCohort(Long cohortId) {
        return cohortRepository.findById(cohortId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validatePendingStatus(AppUser target) {
        if (target.getAccountStatus() != AccountStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "대기 중인 가입 신청이 아닙니다.");
        }
    }

    private void validateVersion(Integer actualVersion, Integer requestedVersion) {
        if (!requestedVersion.equals(actualVersion)) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }
}
