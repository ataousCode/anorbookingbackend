package com.almousleck.service;

import com.almousleck.audit.AuditLog;
import com.almousleck.dto.audit.AuditLogResponse;
import com.almousleck.repository.AuditLogRepository;
import com.almousleck.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findAll(pageable);
        return auditLogs.map(this::convertToAuditLogResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        return auditLogs.map(this::convertToAuditLogResponse);
    }

    @PreAuthorize("hasRole('ADMIN') or #currentUser.id == #userId")
    public Page<AuditLogResponse> getAuditLogsByUser(UserPrincipal currentUser, Long userId, Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByUserId(userId, pageable);
        return auditLogs.map(this::convertToAuditLogResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogResponse> getAuditLogsByAction(String action, Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByAction(action, pageable);
        return auditLogs.map(this::convertToAuditLogResponse);
    }

    private AuditLogResponse convertToAuditLogResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .userId(auditLog.getUserId())
                .username(auditLog.getUsername())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}

