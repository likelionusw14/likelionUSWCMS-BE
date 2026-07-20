package com.likelion.cms.domain.project.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    @GetMapping
    public String getProjects() {
        return "SUCCESS";
    }
}
