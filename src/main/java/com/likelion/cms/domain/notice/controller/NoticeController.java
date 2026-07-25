package com.likelion.cms.domain.notice.controller;

import com.likelion.cms.domain.notice.entity.NoticeTag;
import com.likelion.cms.domain.notice.service.NoticeService;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.domain.notice.dto.response.NoticeResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public PageResponse<NoticeResponse> list(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) NoticeTag tag
    ) {
        return noticeService.list(tag, page, size);
    }

    @GetMapping("/{noticeId}")
    public NoticeResponse get(@PathVariable @Positive Long noticeId) {
        return noticeService.get(noticeId);
    }
}