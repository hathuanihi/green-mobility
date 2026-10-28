package com.greenmobility.modules.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.modules.payment.domain.AuditLog;
import com.greenmobility.modules.payment.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, String actorEmail, String action, String targetType,
                       String targetId, Object beforeState, Object afterState, String ipAddress) {
        try {
            String beforeJson = beforeState != null ? objectMapper.writeValueAsString(beforeState) : null;
            String afterJson = afterState != null ? objectMapper.writeValueAsString(afterState) : null;

            AuditLog auditLog = new AuditLog(actorId, actorEmail, action, targetType, targetId, beforeJson, afterJson, ipAddress);
            auditLogRepository.save(auditLog);
            log.info("Audit log recorded: action={}, actor={}, target={}:{}", action, actorEmail, targetType, targetId);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }
}

