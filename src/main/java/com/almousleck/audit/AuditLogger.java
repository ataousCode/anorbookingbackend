package com.almousleck.audit;


import com.almousleck.model.User;
import com.almousleck.repository.AuditLogRepository;
import com.almousleck.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void logEvent(String action, String entityType, Long entityId, UserPrincipal user, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {}", auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    @Async
    public void logEvent(String action, String entityType, Long entityId, User user, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {}", auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    @Async
    public void logEvent(String action, String entityType, Long entityId, Long userId, String username, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .userId(userId)
                    .username(username)
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {}", auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }
}

