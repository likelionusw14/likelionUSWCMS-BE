package com.likelion.cms.domain.user.controller;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.dto.response.LionResponseDto;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.service.LionService;
import com.likelion.cms.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lions")
public class LionController {

    private final LionService lionService;

    @GetMapping
    public ResponseEntity<PageResponse<LionResponseDto>> getLions(
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) PartType part,
            @RequestParam(required = false) SystemRole role,
            Pageable pageable) {
        return ResponseEntity.ok(lionService.getLions(cohortId, part, role, pageable));
    }
}