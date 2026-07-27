package com.likelion.cms.domain.user.service;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.dto.response.LionResponse;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LionService {

    private final AppUserRepository appUserRepository;

    public PageResponse<LionResponse> getLions(Long cohortId, PartType part, SystemRole role, Pageable pageable) {
        Page<AppUser> lionPage = appUserRepository.findLions(
                AccountStatus.ACTIVE, cohortId, part, role, pageable
        );

        List<LionResponse> items = lionPage.getContent().stream()
                .map(LionResponse::from)
                .toList();

        PageMeta pageMeta = PageMeta.of(
                lionPage.getNumber(),
                lionPage.getSize(),
                lionPage.getTotalElements(),
                lionPage.getTotalPages(),
                lionPage.hasNext()
        );
        return PageResponse.of(items, pageMeta);
    }
}