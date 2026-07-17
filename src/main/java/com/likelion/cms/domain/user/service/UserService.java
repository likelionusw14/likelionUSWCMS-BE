package com.likelion.cms.domain.user.service;

import com.likelion.cms.domain.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository appUserRepository;
}
