package com.greenmobility.common.platform.controller;

import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.common.platform.domain.OutboxEvent;
import com.greenmobility.common.platform.repository.OutboxEventRepository;
import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin Outbox Governance", description = "Quản trị và đẩy lại (Re-drive) các sự kiện Outbox bị lỗi")
@RestController
@RequestMapping("/admin/outbox")
public class AdminOutboxController {

    private final OutboxEventRepository outboxEventRepository;
    private final AuditLogService auditLogService;

    public AdminOutboxController(OutboxEventRepository outboxEventRepository,
                                 AuditLogService auditLogService) {
        this.outboxEventRepository = outboxEventRepository;
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Danh sách các Outbox event đang ở trạng thái FAILED")
    @GetMapping("/failed")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<List<OutboxEvent>>> getFailedEvents() {
        List<OutboxEvent> failed = outboxEventRepository.findByStatusOrderByCreatedAtDesc("FAILED");
        return ResponseEntity.ok(ApiResponse.ok("Danh sách Outbox FAILED", failed));
    }

    @Operation(summary = "Đẩy lại (Re-drive) một Outbox event FAILED")
    @PostMapping("/{id}/redrive")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    @Transactional
    public ResponseEntity<ApiResponse<OutboxEvent>> redriveEvent(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            HttpServletRequest request) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Outbox event: " + id));
        String prevStatus = event.getStatus();
        event.setStatus("NEW");
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        OutboxEvent saved = outboxEventRepository.save(event);

        auditLogService.record(currentUser.getId(), currentUser.getUsername(), "OUTBOX_REDRIVE",
                "OutboxEvent", id.toString(),
                Map.of("status", prevStatus),
                Map.of("status", "NEW"),
                request.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.ok("Đã đưa Outbox event vào hàng đợi phát lại", saved));
    }

    @Operation(summary = "Đẩy lại toàn bộ Outbox event FAILED")
    @PostMapping("/redrive-all")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Integer>>> redriveAllFailed(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request) {
        List<OutboxEvent> failed = outboxEventRepository.findByStatusOrderByCreatedAtDesc("FAILED");
        Instant now = Instant.now();
        for (OutboxEvent event : failed) {
            event.setStatus("NEW");
            event.setAttempts(0);
            event.setNextAttemptAt(now);
        }
        outboxEventRepository.saveAll(failed);

        auditLogService.record(currentUser.getId(), currentUser.getUsername(), "OUTBOX_REDRIVE_ALL",
                "OutboxEvent", "ALL_FAILED",
                Map.of("failedCount", failed.size()),
                Map.of("redrivenCount", failed.size()),
                request.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.ok("Đã đẩy lại toàn bộ Outbox FAILED", Map.of("redrivenCount", failed.size())));
    }
}
