package com.likelion.cms.domain.user.controller;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.dto.response.LionResponse;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.service.LionService;
import com.likelion.cms.global.response.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lions")
public class LionController {

    private final LionService lionService;

    @GetMapping
    public ResponseEntity<PageResponse<LionResponse>> getLions(
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) PartType part,
            @RequestParam(required = false) SystemRole role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(lionService.getLions(cohortId, part, role, pageable));
    }
}