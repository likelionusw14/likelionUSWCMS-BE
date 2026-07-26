package com.likelion.cms.domain.user.service;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LionServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private LionService lionService;

    private Cohort cohort;
    private AppUser member;
    private AppUser admin;

    @BeforeEach
    void setUp() {
        cohort = mock(Cohort.class);
        lenient().when(cohort.getCohortId()).thenReturn(1L);
        lenient().when(cohort.getNumber()).thenReturn(14);
        lenient().when(cohort.getName()).thenReturn("14기");

        member = mock(AppUser.class);
        lenient().when(member.getUserId()).thenReturn(1L);
        lenient().when(member.getName()).thenReturn("홍길동");
        lenient().when(member.getCohort()).thenReturn(cohort);
        lenient().when(member.getPart()).thenReturn(PartType.BACKEND);
        lenient().when(member.getSystemRole()).thenReturn(SystemRole.MEMBER);

        admin = mock(AppUser.class);
        lenient().when(admin.getUserId()).thenReturn(2L);
        lenient().when(admin.getName()).thenReturn("김운영");
        lenient().when(admin.getCohort()).thenReturn(cohort);
        lenient().when(admin.getPart()).thenReturn(PartType.PLANNING);
        lenient().when(admin.getSystemRole()).thenReturn(SystemRole.ADMIN);
    }

    @Test
    @DisplayName("필터 없이 조회하면 ACTIVE 회원 전체가 반환된다")
    void getLions_noFilter_returnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(appUserRepository.findLions(AccountStatus.ACTIVE, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(member, admin), pageable, 2));

        PageResponse<com.likelion.cms.domain.user.dto.response.LionResponse> response =
                lionService.getLions(null, null, null, pageable);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPage().getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("cohortId, part, role 필터를 함께 전달하면 조건에 맞는 결과만 반환된다")
    void getLions_withFilters_returnsFiltered() {
        Pageable pageable = PageRequest.of(0, 20);
        when(appUserRepository.findLions(AccountStatus.ACTIVE, 1L, PartType.BACKEND, SystemRole.MEMBER, pageable))
                .thenReturn(new PageImpl<>(List.of(member), pageable, 1));

        PageResponse<com.likelion.cms.domain.user.dto.response.LionResponse> response =
                lionService.getLions(1L, PartType.BACKEND, SystemRole.MEMBER, pageable);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("MEMBER는 activityType이 BABY_LION으로 매핑된다")
    void getLions_memberRole_mapsToBabyLion() {
        Pageable pageable = PageRequest.of(0, 20);
        when(appUserRepository.findLions(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(member), pageable, 1));

        PageResponse<com.likelion.cms.domain.user.dto.response.LionResponse> response =
                lionService.getLions(null, null, null, pageable);

        assertThat(response.getItems().get(0).getActivityType()).isEqualTo("BABY_LION");
    }

    @Test
    @DisplayName("ADMIN은 activityType이 OPERATOR로 매핑된다")
    void getLions_adminRole_mapsToOperator() {
        Pageable pageable = PageRequest.of(0, 20);
        when(appUserRepository.findLions(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(admin), pageable, 1));

        PageResponse<com.likelion.cms.domain.user.dto.response.LionResponse> response =
                lionService.getLions(null, null, null, pageable);

        assertThat(response.getItems().get(0).getActivityType()).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("결과가 없으면 빈 리스트를 반환한다")
    void getLions_empty_returnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 20);
        when(appUserRepository.findLions(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<com.likelion.cms.domain.user.dto.response.LionResponse> response =
                lionService.getLions(null, null, null, pageable);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPage().getTotalElements()).isZero();
    }
}