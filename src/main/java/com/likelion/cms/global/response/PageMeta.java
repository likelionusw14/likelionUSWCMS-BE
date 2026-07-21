package com.likelion.cms.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 목록 API 공통 페이지 정보. 프론트가 "다음 페이지 버튼을 보여줄지"를
// totalPages 계산 없이 hasNext 값만 보고 바로 판단할 수 있게 미리 계산해서 내려줌.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageMeta {

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;

    public static PageMeta of(int page, int size, long totalElements, int totalPages, boolean hasNext) {
        return new PageMeta(page, size, totalElements, totalPages, hasNext);
    }
}
