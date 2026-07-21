package com.likelion.cms.global.security;

import com.likelion.cms.domain.user.entity.SystemRole;

// 로그인한 사용자를 나타내는 자리표시자. @AuthenticationPrincipal로 컨트롤러에 주입됨.
// 실제 인증(JWT/카카오 로그인) 인프라가 아직 없어서 지금은 항상 null로 들어옴 -
// 나중에 인증 기능이 이 record를 채워서 Authentication에 넣어주기만 하면
// 이 코드는 손댈 필요 없이 바로 동작하도록 미리 분리해둔 것.
public record CurrentUserPrincipal(Long userId, SystemRole role) {
}
