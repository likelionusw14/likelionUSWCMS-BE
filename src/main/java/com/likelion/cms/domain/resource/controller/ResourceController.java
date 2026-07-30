package com.likelion.cms.domain.resource.controller;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.resource.dto.response.LearningResourceResponse;
import com.likelion.cms.domain.resource.service.ResourceService;
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
import com.likelion.cms.support.file.dto.response.DownloadUrlResponse;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public PageResponse<LearningResourceResponse> getResources(
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) PartType targetPart,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive int size
    ) {
        return resourceService.getResources(week, targetPart, page, size);
    }

    @GetMapping("/{resourceId}/download-url")
    public DownloadUrlResponse getDownloadUrl(@PathVariable @Positive Long resourceId) {
        return resourceService.getDownloadUrl(resourceId);
    }

    @GetMapping("/{resourceId}")
    public LearningResourceResponse getResource(@PathVariable @Positive Long resourceId) {
        return resourceService.getResource(resourceId);
    }
}
