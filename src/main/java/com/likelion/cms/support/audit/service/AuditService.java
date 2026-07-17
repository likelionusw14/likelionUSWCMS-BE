package com.likelion.cms.support.audit.service;

import com.likelion.cms.support.audit.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;
}
