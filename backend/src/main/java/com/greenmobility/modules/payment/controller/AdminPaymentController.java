package com.greenmobility.modules.payment.controller;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.payment.domain.*;
import com.greenmobility.modules.payment.dto.*;
import com.greenmobility.modules.payment.gateway.GatewayQueryResult;
import com.greenmobility.modules.payment.gateway.PaymentGateway;
import com.greenmobility.modules.payment.ledger.LedgerAccountRepository;
import com.greenmobility.modules.payment.ledger.LedgerEntryRepository;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.repository.PaymentWebhookEventRepository;
import com.greenmobility.modules.payment.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin Financial Operations", description = "APIs for Admin Payment Monitoring, Refunds, Withdrawal Approvals and Reconciliation")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_FINANCE', 'FINANCE')")
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final WithdrawalService withdrawalService;
    private final FinanceReportService financeReportService;
    private final PlatformSettingsService settingsService;
    private final LedgerEntryRepository entryRepository;
    private final LedgerAccountRepository accountRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final AuditLogService auditLogService;

    public AdminPaymentController(PaymentRepository paymentRepository,
                                  PaymentService paymentService,
                                  RefundService refundService,
                                  WithdrawalService withdrawalService,
                                  FinanceReportService financeReportService,
                                  PlatformSettingsService settingsService,
                                  LedgerEntryRepository entryRepository,
                                  LedgerAccountRepository accountRepository,
                                  PaymentWebhookEventRepository webhookEventRepository,
                                  AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.withdrawalService = withdrawalService;
        this.financeReportService = financeReportService;
        this.settingsService = settingsService;
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Tìm kiếm & Lọc danh sách giao dịch thanh toán")
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<Page<PaymentDto>>> searchPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Specification<Payment> spec = (root, query, cb) -> cb.conjunction();
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (method != null && !method.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("method"), method));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        Page<PaymentDto> result = paymentRepository.findAll(spec, pageable)
                .map(paymentService::toDto);
        return ResponseEntity.ok(ApiResponse.ok("Danh sách thanh toán", result));
    }

    @Operation(summary = "Xem chi tiết thanh toán kèm bút toán sổ cái và nhật ký webhook")
    @GetMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<AdminFinanceDtos.AdminPaymentDetailResponse>> getPaymentDetail(
            @PathVariable UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán"));

        PaymentDto pDto = paymentService.toDto(payment);

        List<AdminFinanceDtos.LedgerEntryDetail> entryDetails = List.of();
        if (payment.getLedgerTransactionId() != null) {
            entryDetails = entryRepository.findByTransactionId(payment.getLedgerTransactionId()).stream()
                    .map(e -> {
                        LedgerAccount acc = accountRepository.findById(e.getAccountId()).orElse(null);
                        String accType = acc != null ? acc.getAccountType().name() : "UNKNOWN";
                        String accNum = acc != null ? acc.getAccountNumber() : "UNKNOWN";
                        return new AdminFinanceDtos.LedgerEntryDetail(
                                e.getId(),
                                accType,
                                accNum,
                                e.getEntryType().name(),
                                e.getAmount().longValue(),
                                e.getBalanceAfter() != null ? e.getBalanceAfter().longValue() : 0,
                                e.getCreatedAt()
                        );
                    }).toList();
        }

        List<AdminFinanceDtos.WebhookEventDetail> webhookDetails = List.of();
        if (payment.getGateway() != null && payment.getGatewayTxnRef() != null) {
            webhookDetails = webhookEventRepository
                    .findByGatewayAndGatewayTxnRefOrderByReceivedAtDesc(payment.getGateway(), payment.getGatewayTxnRef())
                    .stream()
                    .map(w -> new AdminFinanceDtos.WebhookEventDetail(
                            w.getId(),
                            w.getGateway(),
                            w.getGatewayTxnRef(),
                            w.isSignatureValid(),
                            w.getProcessingResult(),
                            w.getSourceIp(),
                            w.getReceivedAt()
                    )).toList();
        }

        AdminFinanceDtos.AdminPaymentDetailResponse detail =
                new AdminFinanceDtos.AdminPaymentDetailResponse(pDto, entryDetails, webhookDetails);

        return ResponseEntity.ok(ApiResponse.ok("Chi tiết thanh toán", detail));
    }

    @Operation(summary = "Admin thực hiện hoàn tiền cuốc xe (Toàn phần hoặc Một phần)")
    @PostMapping("/payments/{id}/refund")
    public ResponseEntity<ApiResponse<Void>> refundPayment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @RequestBody AdminFinanceDtos.RefundRequest request,
            HttpServletRequest httpReq) {
        String ip = httpReq.getRemoteAddr();
        refundService.refund(currentUser.getId(), currentUser.getUsername(), id, request, ip);
        return ResponseEntity.ok(ApiResponse.ok("Hoàn tiền thành công", null));
    }

    @Operation(summary = "Requery trạng thái giao dịch từ cổng thanh toán đối soát")
    @PostMapping("/payments/{id}/requery")
    public ResponseEntity<ApiResponse<GatewayQueryResult>> requeryPayment(
            @PathVariable UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán"));

        if (payment.getGateway() == null || payment.getGatewayTxnRef() == null) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PAYMENT_NOT_ONLINE", "Giao dịch này không thực hiện qua cổng online");
        }

        PaymentGateway gateway = paymentService.selectGateway(payment.getGateway());
        GatewayQueryResult result = gateway.queryTransaction(payment.getGatewayTxnRef());

        if (result.isSuccess() && !"PAID".equals(payment.getStatus())) {
            paymentService.settle(payment.getId(), "ADMIN_REQUERY", result.gatewayTransactionNo());
        }

        return ResponseEntity.ok(ApiResponse.ok("Kết quả truy vấn cổng thanh toán", result));
    }

    @Operation(summary = "Lấy danh sách yêu cầu rút tiền của tài xế")
    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<Page<WithdrawalDtos.WithdrawalResponse>>> getWithdrawals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<WithdrawalDtos.WithdrawalResponse> result = withdrawalService.getAdminWithdrawals(status, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Danh sách yêu cầu rút tiền", result));
    }

    @Operation(summary = "Phê duyệt yêu cầu rút tiền (Đã chuyển khoản thành công)")
    @PostMapping("/withdrawals/{id}/approve")
    public ResponseEntity<ApiResponse<WithdrawalDtos.WithdrawalResponse>> approveWithdrawal(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @RequestBody(required = false) WithdrawalDtos.ApproveWithdrawalRequest req,
            HttpServletRequest httpReq) {
        String bankRef = req != null ? req.bankReference() : "MANUAL-TRANSFER";
        WithdrawalDtos.WithdrawalResponse res = withdrawalService.approveWithdrawal(
                currentUser.getId(), currentUser.getUsername(), id, bankRef, httpReq.getRemoteAddr()
        );
        return ResponseEntity.ok(ApiResponse.ok("Duyệt yêu cầu rút tiền thành công", res));
    }

    @Operation(summary = "Từ chối yêu cầu rút tiền (Hoàn tiền giữ lại về ví thu nhập tài xế)")
    @PostMapping("/withdrawals/{id}/reject")
    public ResponseEntity<ApiResponse<WithdrawalDtos.WithdrawalResponse>> rejectWithdrawal(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @RequestBody WithdrawalDtos.RejectWithdrawalRequest req,
            HttpServletRequest httpReq) {
        WithdrawalDtos.WithdrawalResponse res = withdrawalService.rejectWithdrawal(
                currentUser.getId(), currentUser.getUsername(), id, req.reason(), httpReq.getRemoteAddr()
        );
        return ResponseEntity.ok(ApiResponse.ok("Từ chối yêu cầu rút tiền thành công", res));
    }

    @Operation(summary = "Báo cáo tổng quan tài chính (GMV, Doanh thu hoa hồng, Tỷ lệ thành công)")
    @GetMapping("/finance/overview")
    public ResponseEntity<ApiResponse<AdminFinanceDtos.FinanceOverviewResponse>> getFinanceOverview() {
        AdminFinanceDtos.FinanceOverviewResponse overview = financeReportService.getOverview();
        return ResponseEntity.ok(ApiResponse.ok("Tổng quan tài chính", overview));
    }

    @Operation(summary = "Đối soát toàn vẹn sổ cái kế toán kép (Reconciliation)")
    @GetMapping("/finance/reconciliation")
    public ResponseEntity<ApiResponse<AdminFinanceDtos.ReconciliationResponse>> getReconciliation() {
        AdminFinanceDtos.ReconciliationResponse res = financeReportService.getReconciliation();
        return ResponseEntity.ok(ApiResponse.ok("Kết quả đối soát sổ cái", res));
    }

    @Operation(summary = "Lấy cấu hình tham số thanh toán và phí hoa hồng")
    @GetMapping("/settings/payment")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPaymentSettings() {
        Map<String, String> settings = settingsService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.ok("Cấu hình tài chính", settings));
    }

    @Operation(summary = "Cập nhật tham số tài chính (Tỷ lệ hoa hồng, hạn mức nạp/rút)")
    @PutMapping("/settings/payment")
    public ResponseEntity<ApiResponse<Map<String, String>>> updatePaymentSettings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody Map<String, String> updates,
            HttpServletRequest httpReq) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            settingsService.updateSetting(entry.getKey(), entry.getValue(), currentUser.getId());
        }
        auditLogService.record(currentUser.getId(), currentUser.getUsername(), "UPDATE_SETTINGS",
                "PlatformSetting", "payment_settings", null, updates, httpReq.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật tham số thành công", settingsService.getAllSettings()));
    }
}

