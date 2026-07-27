package com.likelion.cms.domain.project.controller;

import com.likelion.cms.common.type.ProjectType;
import com.likelion.cms.domain.project.dto.response.ProjectResponse;
import com.likelion.cms.domain.project.service.ProjectService;
import com.likelion.cms.global.response.PageResponse;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public PageResponse<ProjectResponse> getProjects(
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) List<ProjectType> projectType,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive int size
    ) {
        return projectService.getProjects(cohortId, projectType, page, size);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(@PathVariable @Positive Long projectId) {
        return projectService.getProject(projectId);
    }
}
