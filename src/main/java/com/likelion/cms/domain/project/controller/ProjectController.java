package com.likelion.cms.domain.project.controller;

import com.likelion.cms.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<Object>> getProjects() {
        List<Object> projects = projectService.findAllProjects();
        return ResponseEntity.ok(projects);
    }
}
