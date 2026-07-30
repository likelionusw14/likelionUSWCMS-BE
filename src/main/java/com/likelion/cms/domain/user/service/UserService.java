package com.likelion.cms.domain.user.service;

import com.likelion.cms.common.type.PartType;
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
import com.likelion.cms.domain.user.repository.AppUserSpecs;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 클래스 레벨은 기본 readOnly 트랜잭션이고, 실제로 상태를 바꾸는 메서드에만
// @Transactional을 따로 붙여서 쓰기 트랜잭션으로 승격시킴 (조회 성능 최적화 목적).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository appUserRepository;
    private final CohortRepository cohortRepository;

    // 목록 조회: status/role/cohortId/part/keyword 모두 선택 필터. 스펙 기준으로
    // 정렬은 항상 "가입 신청일(createdAt) 내림차순" 고정 - 클라이언트가 sort를 지정해도 무시.
    public PageResponse<AccountResponse> list(
            AccountStatus status, SystemRole role, Long cohortId, PartType part, String keyword, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AppUser> accounts = appUserRepository.findAll(
                AppUserSpecs.withFilters(status, role, cohortId, part, keyword), sortedPageable);
        return PageResponse.from(accounts.map(AccountResponse::from));
    }

    // 단건 조회.
    public AccountResponse get(Long userId) {
        return AccountResponse.from(findAccount(userId));
    }

    // 가입 승인. 대기 중(PENDING)이 아닌 계정을 다시 승인하는 건 막음.
    @Transactional
    public AccountResponse approve(Long userId, ApproveAccountRequest request, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        AppUser target = findAccount(userId);
        validatePendingStatus(target);
        validateVersion(target.getVersion(), request.version());
        target.approve(actor);
        // @Version은 실제 UPDATE가 나가야(flush) 엔티티의 메모리 값도 올라감.
        // flush 없이 바로 응답을 만들면 version이 갱신 전 값으로 내려가서,
        // 클라이언트가 그 값으로 바로 다음 PATCH를 보내면 낙관적 락 충돌이 남.
        appUserRepository.flush();
        return AccountResponse.from(target);
    }

    // 가입 거절. 승인과 동일하게 PENDING 상태에서만 가능.
    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public AccountResponse reject(Long userId, RejectAccountRequest request, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        AppUser target = findAccount(userId);
        validatePendingStatus(target);
        validateVersion(target.getVersion(), request.version());
        target.reject(actor, request.rejectionReason().trim());
        appUserRepository.flush();
        return AccountResponse.from(target);
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public AccountResponse changeRole(Long userId, UpdateRoleRequest request, Long actorUserId) {
        requireActorExists(actorUserId);
        AppUser target = findAccount(userId);
        validateVersion(target.getVersion(), request.version());
        // 스펙 규칙: ACTIVE 회원만 권한 변경 가능.
        if (target.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "ACTIVE 상태의 회원만 권한을 변경할 수 있습니다.");
        }
        // 스펙 규칙: 마지막 ADMIN을 MEMBER로 변경하는 건 차단 (관리 기능이 잠기는 걸 방지).
        if (target.getSystemRole() == SystemRole.ADMIN
                && request.role() == SystemRole.MEMBER
                && appUserRepository.countBySystemRole(SystemRole.ADMIN) <= 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "마지막 관리자는 권한을 해제할 수 없습니다.");
        }
        target.changeRole(request.role());
        appUserRepository.flush();
        return AccountResponse.from(target);
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public AccountResponse update(Long userId, UpdateAccountRequest request, Long actorUserId) {
        requireActorExists(actorUserId);
        AppUser target = findAccount(userId);
        validateVersion(target.getVersion(), request.getVersion());

        // 부분 수정: 요청에 "포함된" 필드만 반영. 클라이언트가 값을 안 보낸 필드는
        // 기존 값을 그대로 유지해야 하므로, request의 xxxProvided() 플래그로 판단.
        if (request.isNameProvided()) {
            target.updateName(request.getName().trim());
        }
        if (request.isDepartmentProvided()) {
            target.updateDepartment(request.getDepartment().trim());
        }
        if (request.isStudentIdProvided()) {
            target.updateStudentId(request.getStudentId().trim());
        }
        if (request.isPartProvided()) {
            target.updatePart(request.getPart());
        }
        if (request.isCohortIdProvided()) {
            target.updateCohort(findCohort(request.getCohortId()));
        }

        appUserRepository.flush();
        return AccountResponse.from(target);
    }

    @Transactional
    public void delete(Long userId, Long actorUserId) {
        requireActorExists(actorUserId);
        AppUser target = findAccount(userId);
        // 스펙 규칙: 마지막 ADMIN은 삭제 불가.
        if (target.getSystemRole() == SystemRole.ADMIN
                && appUserRepository.countBySystemRole(SystemRole.ADMIN) <= 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "마지막 관리자는 삭제할 수 없습니다.");
        }
        target.delete();
    }

    // 요청을 보낸 사람(관리자) 자신이 실제 DB에 존재하는 유효한 계정인지 확인.
    // 지금은 인증 인프라가 없어서 항상 정상 케이스만 타지만, 나중에 인증이 붙으면
    // 위조/탈퇴된 계정의 토큰으로 요청이 와도 여기서 걸러짐.
    private AppUser findActor(Long actorUserId) {
        return appUserRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    // findActor와 하는 검증은 같지만, 조회한 AppUser 자체가 필요 없는 메서드용.
    // existsById로 존재 여부만 확인해서 "반환값을 버린다"는 인상 자체를 없앰.
    private void requireActorExists(Long actorUserId) {
        if (!appUserRepository.existsById(actorUserId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 처리 대상 회원 조회. @SQLRestriction 덕분에 소프트 삭제된 회원은 자동으로 빠짐.
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

    // 낙관적 락: 클라이언트가 "내가 마지막으로 조회했을 때의 버전"을 같이 보내고,
    // 그 사이 누가 먼저 수정해서 버전이 바뀌었으면 충돌로 처리 (덮어쓰기 방지).
    private void validateVersion(Integer actualVersion, Integer requestedVersion) {
        if (!requestedVersion.equals(actualVersion)) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }
}
