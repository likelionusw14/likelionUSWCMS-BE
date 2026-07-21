package com.likelion.cms.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

// 목록 API의 공통 응답 포맷: { items: [...], page: {...} }
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private final List<T> items;
    private final PageMeta page;

    public static <T> PageResponse<T> of(List<T> items, PageMeta page) {
        return new PageResponse<>(items, page);
    }

    // Spring Data가 리턴하는 Page<T>를 이 포맷으로 바로 바꿔주는 헬퍼.
    // 서비스 코드에서 매번 PageMeta.of(...)를 손으로 조립하지 않아도 되게 함.
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                PageMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(),
                        page.getTotalPages(), page.hasNext())
        );
    }
}
