package com.likelion.cms.global.security;

import com.likelion.cms.domain.user.entity.SystemRole;

public record CurrentUserPrincipal(Long userId, SystemRole role) {
}
