package com.likelion.cms.support.audit.repository;

import com.likelion.cms.support.audit.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
