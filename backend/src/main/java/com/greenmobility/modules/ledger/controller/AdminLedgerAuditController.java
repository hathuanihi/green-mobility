package com.greenmobility.modules.ledger.controller;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.common.platform.service.IdempotencyService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.ledger.api.LedgerAuditReportDto;
import com.greenmobility.modules.ledger.api.LedgerModuleApi;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import com.greenmobility.modules.ledger.domain.LedgerEntry;
import com.greenmobility.modules.ledger.domain.LedgerTransaction;
import com.greenmobility.modules.ledger.domain.LedgerTxType;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.ledger.repository.LedgerEntryRepository;
import com.greenmobility.modules.ledger.repository.LedgerTransactionRepository;
import com.greenmobility.modules.ledger.service.LedgerAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Tag(name = "Admin Double-Entry Ledger Governance", description = "Kiểm toán sổ cái kép đa tiền tệ, tra cứu bút toán, đảo giao dịch và Circuit-Breaker")
@RestController
@RequestMapping("/admin/ledger")
public class AdminLedgerAuditController {

    private final LedgerAuditService ledgerAuditService;
    private final LedgerModuleApi ledgerModuleApi;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerAccountRepository accountRepository;
    private final PlatformSettingsService settingsService;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;

    public AdminLedgerAuditController(LedgerAuditService ledgerAuditService,
                                      LedgerModuleApi ledgerModuleApi,
                                      LedgerTransactionRepository transactionRepository,
                                      LedgerEntryRepository entryRepository,
                                      LedgerAccountRepository accountRepository,
                                      PlatformSettingsService settingsService,
                                      AuditLogService auditLogService,
                                      IdempotencyService idempotencyService) {
        this.ledgerAuditService = ledgerAuditService;
        this.ledgerModuleApi = ledgerModuleApi;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
        this.idempotencyService = idempotencyService;
    }

    public record LedgerTransactionSummaryDto(
            UUID id,
            String transactionCode,
            UUID tripId,
            String transactionType,
            String currency,
            BigDecimal totalAmount,
            String description,
            String referenceType,
            UUID referenceId,
            UUID reversalOf,
            boolean alreadyReversed,
            Instant createdAt
    ) {}

    public record LedgerEntryViewDto(
            UUID id,
            UUID accountId,
            String accountNumber,
            String accountType,
            String currency,
            String entryType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Instant createdAt
    ) {}

    public record LedgerTransactionDetailDto(
            LedgerTransactionSummaryDto transaction,
            List<LedgerEntryViewDto> entries
    ) {}

    public record ReverseTransactionRequest(String reason) {}

    public record CircuitBreakerRequest(boolean frozen, String reason) {}

    @Operation(summary = "Kiểm toán toàn vẹn sổ cái kép đa tiền tệ (VND / PCC / ECO_POINT)")
    @GetMapping("/audit")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LedgerAuditReportDto>> getAuditReport(
            @RequestParam(defaultValue = "false") boolean autoCircuitBreak) {
        LedgerAuditReportDto report = ledgerAuditService.runFullAudit(autoCircuitBreak);
        return ResponseEntity.ok(ApiResponse.ok("Báo cáo kiểm toán sổ cái đa tiền tệ", report));
    }

    @Operation(summary = "Tra cứu danh sách giao dịch sổ cái có phân trang & lọc")
    @GetMapping("/transactions")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<Page<LedgerTransactionSummaryDto>>> searchTransactions(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String txCode,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Set<UUID> filterTxIds = null;
        if ((currency != null && !currency.isBlank()) || userId != null) {
            List<UUID> accIds = accountRepository.findAccountIdsByFilter(
                    currency != null && !currency.isBlank() ? currency.trim() : null,
                    userId
            );
            if (accIds.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok("Danh sách giao dịch sổ cái", Page.empty(pageable)));
            }
            filterTxIds = new HashSet<>(transactionRepository.findTransactionIdsByAccountIds(accIds));
            if (filterTxIds.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok("Danh sách giao dịch sổ cái", Page.empty(pageable)));
            }
        }

        final Set<UUID> txIdSet = filterTxIds;
        Specification<LedgerTransaction> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (type != null && !type.isBlank()) {
                preds.add(cb.equal(root.get("transactionType"), LedgerTxType.valueOf(type.trim().toUpperCase())));
            }
            if (txCode != null && !txCode.isBlank()) {
                preds.add(cb.like(cb.upper(root.get("transactionCode")), "%" + txCode.trim().toUpperCase() + "%"));
            }
            if (tripId != null) {
                preds.add(cb.equal(root.get("tripId"), tripId));
            }
            if (txIdSet != null) {
                preds.add(root.get("id").in(txIdSet));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };

        Page<LedgerTransactionSummaryDto> result = transactionRepository.findAll(spec, pageable)
                .map(this::toSummaryDto);
        return ResponseEntity.ok(ApiResponse.ok("Danh sách giao dịch sổ cái", result));
    }

    @Operation(summary = "Xem chi tiết bút toán Debit/Credit của 1 giao dịch sổ cái")
    @GetMapping("/transactions/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LedgerTransactionDetailDto>> getTransactionDetail(@PathVariable UUID id) {
        LedgerTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "LEDGER_TX_NOT_FOUND",
                        "Không tìm thấy giao dịch sổ cái: " + id));
        List<LedgerEntry> entries = entryRepository.findByTransactionId(id);
        List<LedgerEntryViewDto> entryViews = entries.stream().map(e -> {
            LedgerAccount acc = accountRepository.findById(e.getAccountId()).orElse(null);
            return new LedgerEntryViewDto(
                    e.getId(),
                    e.getAccountId(),
                    acc != null ? acc.getAccountNumber() : "UNKNOWN",
                    acc != null ? acc.getAccountType().name() : "UNKNOWN",
                    acc != null ? acc.getCurrency() : "UNKNOWN",
                    e.getEntryType().name(),
                    e.getAmount(),
                    e.getBalanceAfter(),
                    e.getCreatedAt()
            );
        }).toList();

        return ResponseEntity.ok(ApiResponse.ok("Chi tiết bút toán sổ cái",
                new LedgerTransactionDetailDto(toSummaryDto(tx), entryViews)));
    }

    @Operation(summary = "Admin đảo giao dịch sổ cái nguyên tử (ADMIN_REVERSAL)")
    @PostMapping("/transactions/{id}/reverse")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    public ResponseEntity<ApiResponse<LedgerTransactionSummaryDto>> reverseTransaction(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ReverseTransactionRequest body,
            HttpServletRequest request) {
        return idempotencyService.executeWithIdempotency(
                idempotencyKey,
                currentUser.getId(),
                "/admin/ledger/transactions/" + id + "/reverse",
                body,
                (Class<ApiResponse<LedgerTransactionSummaryDto>>) (Class<?>) ApiResponse.class,
                true,
                () -> {
                    LedgerTransaction rev = ledgerModuleApi.reverseTransaction(
                            currentUser.getId(), currentUser.getUsername(), id,
                            body != null ? body.reason() : null, request.getRemoteAddr()
                    );
                    return ResponseEntity.ok(ApiResponse.ok("Đảo giao dịch sổ cái thành công", toSummaryDto(rev)));
                }
        );
    }

    @Operation(summary = "Bật/tắt Circuit-Breaker khóa đổi thưởng toàn hệ thống")
    @PostMapping("/circuit-breaker")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleCircuitBreaker(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody CircuitBreakerRequest body,
            HttpServletRequest request) {
        boolean prev = settingsService.isRedemptionsFrozen();
        settingsService.updateSetting("redemptions_frozen", String.valueOf(body.frozen()), currentUser.getId());
        auditLogService.record(currentUser.getId(), currentUser.getUsername(), "TOGGLE_CIRCUIT_BREAKER",
                "PlatformSetting", "redemptions_frozen",
                Map.of("redemptions_frozen", prev),
                Map.of("redemptions_frozen", body.frozen(), "reason", body.reason() != null ? body.reason() : ""),
                request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái Circuit-Breaker thành công",
                Map.of("redemptionsFrozen", body.frozen())));
    }

    @Operation(summary = "Danh sách các tài khoản ví đang bị đóng băng (FROZEN)")
    @GetMapping("/frozen-accounts")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<List<LedgerAccount>>> getFrozenAccounts() {
        return ResponseEntity.ok(ApiResponse.ok("Danh sách tài khoản đang đóng băng",
                accountRepository.findByStatusOrderByCreatedAtDesc("FROZEN")));
    }

    private LedgerTransactionSummaryDto toSummaryDto(LedgerTransaction tx) {
        List<LedgerEntry> entries = entryRepository.findByTransactionId(tx.getId());
        String currency = "VND";
        BigDecimal totalDebit = BigDecimal.ZERO;
        for (LedgerEntry e : entries) {
            if ("DEBIT".equals(e.getEntryType().name())) {
                totalDebit = totalDebit.add(e.getAmount());
            }
        }
        if (!entries.isEmpty()) {
            currency = accountRepository.findById(entries.get(0).getAccountId())
                    .map(LedgerAccount::getCurrency).orElse("VND");
        }
        boolean reversed = transactionRepository.existsByReversalOf(tx.getId());
        return new LedgerTransactionSummaryDto(
                tx.getId(),
                tx.getTransactionCode(),
                tx.getTripId(),
                tx.getTransactionType().name(),
                currency,
                totalDebit,
                tx.getDescription(),
                tx.getReferenceType(),
                tx.getReferenceId(),
                tx.getReversalOf(),
                reversed,
                tx.getCreatedAt()
        );
    }
}
