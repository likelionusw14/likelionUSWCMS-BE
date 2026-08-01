package com.likelion.cms.domain.project.service;

import com.likelion.cms.common.type.ProjectType;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.project.dto.request.CreateProjectRequest;
import com.likelion.cms.domain.project.dto.request.ProjectParticipantRequest;
import com.likelion.cms.domain.project.dto.request.UpdateProjectRequest;
import com.likelion.cms.domain.project.dto.response.AdminProjectResponse;
import com.likelion.cms.domain.project.entity.Project;
import com.likelion.cms.domain.project.repository.ProjectParticipationRepository;
import com.likelion.cms.domain.project.repository.ProjectRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectParticipationRepository projectParticipationRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private FileAssetRepository fileAssetRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository, projectParticipationRepository, cohortRepository, fileAssetRepository, appUserRepository);
    }

    @Test
    void createSavesProjectWithoutOptionalThumbnail() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        CreateProjectRequest request = new CreateProjectRequest(
                " 새 프로젝트 ", " 설명 ", ProjectType.HACKATHON, null, "https://example.com", "https://github.com/example/repo",
                5L, YearMonth.of(2026, 1), YearMonth.of(2026, 6), List.of()
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            initializeEntity(project, 30L, 0);
            return project;
        });

        AdminProjectResponse response = projectService.create(request, 1L);

        assertThat(response.getProjectId()).isEqualTo(30L);
        assertThat(response.getTitle()).isEqualTo("새 프로젝트");
        assertThat(response.getThumbnailAssetId()).isNull();
        assertThat(response.getParticipants()).isEmpty();
        verify(fileAssetRepository, never()).findById(any());
    }

    // 등록 시 참여자가 실제로 저장되는지 - 원래 요청에서 아예 안 받아서 항상 비어 있던 부분.
    @Test
    void createSavesParticipants() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        AppUser participant = participant(11L, "홍길동");
        CreateProjectRequest request = new CreateProjectRequest(
                "새 프로젝트", "설명", ProjectType.HACKATHON, null, null, null,
                5L, YearMonth.of(2026, 1), YearMonth.of(2026, 6),
                List.of(new ProjectParticipantRequest(11L, " 팀장 "))
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(appUserRepository.findAllById(List.of(11L))).thenReturn(List.of(participant));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            initializeEntity(project, 30L, 0);
            return project;
        });
        when(projectParticipationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminProjectResponse response = projectService.create(request, 1L);

        assertThat(response.getParticipants()).hasSize(1);
        assertThat(response.getParticipants().get(0).getUserId()).isEqualTo(11L);
        assertThat(response.getParticipants().get(0).getName()).isEqualTo("홍길동");
        assertThat(response.getParticipants().get(0).getRole()).isEqualTo("팀장");
    }

    @Test
    void createRejectsUnknownParticipantUser() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        CreateProjectRequest request = new CreateProjectRequest(
                "새 프로젝트", "설명", ProjectType.HACKATHON, null, null, null,
                5L, YearMonth.of(2026, 1), YearMonth.of(2026, 6),
                List.of(new ProjectParticipantRequest(99L, "팀장"))
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(appUserRepository.findAllById(List.of(99L))).thenReturn(List.of());
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            initializeEntity(project, 30L, 0);
            return project;
        });

        assertThatThrownBy(() -> projectService.create(request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(projectParticipationRepository, never()).saveAll(anyList());
    }

    @Test
    void createRejectsMissingCohort() {
        AppUser actor = actor(1L);
        CreateProjectRequest request = new CreateProjectRequest(
                "프로젝트", "설명", ProjectType.HACKATHON, null, null, null,
                5L, YearMonth.of(2026, 1), YearMonth.of(2026, 6), List.of()
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.create(request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void createRejectsThumbnailWithWrongPurpose() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        FileAsset wrongPurposeAsset = fileAsset(FilePurpose.NOTICE_IMAGE);
        CreateProjectRequest request = new CreateProjectRequest(
                "프로젝트", "설명", ProjectType.HACKATHON, 10L, null, null,
                5L, YearMonth.of(2026, 1), YearMonth.of(2026, 6), List.of()
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(fileAssetRepository.findById(10L)).thenReturn(Optional.of(wrongPurposeAsset));

        assertThatThrownBy(() -> projectService.create(request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        Project project = project(actor, cohort, null);
        initializeEntity(project, 30L, 0);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setVersion(0);
        request.setTitle(" 수정된 제목 ");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));

        AdminProjectResponse response = projectService.update(30L, request, 1L);

        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getDescription()).isEqualTo("기존 설명");
    }

    @Test
    void updateRejectsVersionMismatch() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        Project project = project(actor, cohort, null);
        initializeEntity(project, 30L, 1);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setVersion(0);
        request.setTitle("수정된 제목");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.update(30L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    @Test
    void updateRejectsInvalidDateRange() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        Project project = project(actor, cohort, null);
        initializeEntity(project, 30L, 0);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setVersion(0);
        request.setEndedMonth(YearMonth.of(2025, 1));

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.update(30L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    // participants를 보내면 기존 참여자를 지우고 보낸 목록으로 통째로 교체한다.
    @Test
    void updateReplacesParticipants() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        Project project = project(actor, cohort, null);
        initializeEntity(project, 30L, 0);
        AppUser participant = participant(11L, "홍길동");

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setVersion(0);
        request.setParticipants(List.of(new ProjectParticipantRequest(11L, "백엔드")));

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));
        when(appUserRepository.findAllById(List.of(11L))).thenReturn(List.of(participant));
        when(projectParticipationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminProjectResponse response = projectService.update(30L, request, 1L);

        verify(projectParticipationRepository).deleteByProjectId(30L);
        assertThat(response.getParticipants()).hasSize(1);
        assertThat(response.getParticipants().get(0).getRole()).isEqualTo("백엔드");
    }

    // participants를 안 보내면 기존 참여자는 건드리지 않고 응답에만 실어준다.
    @Test
    void updateKeepsParticipantsWhenNotProvided() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        Project project = project(actor, cohort, null);
        initializeEntity(project, 30L, 0);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setVersion(0);
        request.setTitle("수정된 제목");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));

        projectService.update(30L, request, 1L);

        verify(projectParticipationRepository, never()).deleteByProjectId(any());
        verify(projectParticipationRepository).findByProjectIdWithUser(30L);
    }

    @Test
    void deleteSoftDeletesProject() {
        AppUser actor = actor(1L);
        Cohort cohort = cohort(5L);
        Project project = project(actor, cohort, null);
        initializeEntity(project, 30L, 0);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));

        projectService.delete(30L, 1L);

        assertThat(project.getDeletedAt()).isNotNull();
    }

    private AppUser actor(Long userId) {
        AppUser actor = mock(AppUser.class);
        lenient().when(actor.getUserId()).thenReturn(userId);
        return actor;
    }

    private Cohort cohort(Long cohortId) {
        Cohort cohort = mock(Cohort.class);
        lenient().when(cohort.getCohortId()).thenReturn(cohortId);
        lenient().when(cohort.getNumber()).thenReturn(5);
        lenient().when(cohort.getName()).thenReturn("5기");
        return cohort;
    }

    private FileAsset fileAsset(FilePurpose purpose) {
        FileAsset fileAsset = mock(FileAsset.class);
        lenient().when(fileAsset.getPurpose()).thenReturn(purpose);
        return fileAsset;
    }

    private Project project(AppUser actor, Cohort cohort, FileAsset thumbnailAsset) {
        return Project.builder()
                .title("기존 제목")
                .description("기존 설명")
                .projectType(ProjectType.HACKATHON.name())
                .thumbnailAsset(thumbnailAsset)
                .deployUrl("https://example.com/old")
                .githubUrl("https://github.com/example/old")
                .cohort(cohort)
                .startedMonth(LocalDate.of(2026, 1, 1))
                .endedMonth(LocalDate.of(2026, 6, 1))
                .createdByUser(actor)
                .build();
    }

    private AppUser participant(Long userId, String name) {
        AppUser user = mock(AppUser.class);
        lenient().when(user.getUserId()).thenReturn(userId);
        lenient().when(user.getName()).thenReturn(name);
        return user;
    }

    private void initializeEntity(Project project, Long projectId, Integer version) {
        ReflectionTestUtils.setField(project, "projectId", projectId);
        ReflectionTestUtils.setField(project, "version", version);
        ReflectionTestUtils.setField(project, "createdAt", LocalDateTime.of(2026, 7, 21, 10, 0));
        ReflectionTestUtils.setField(project, "updatedAt", LocalDateTime.of(2026, 7, 21, 10, 0));
    }
}
