package com.likelion.cms.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class PageResponse<T>{
    private final List<T> items;
    private final PageMeta page;

    public static <T> PageResponse<T> of(List<T> items, PageMeta page)
    {
        return new PageResponse<>(items,page);
    }

}
