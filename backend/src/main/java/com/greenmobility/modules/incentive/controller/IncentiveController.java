package com.greenmobility.modules.incentive.controller;

import com.greenmobility.common.platform.service.IdempotencyService;
import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.incentive.api.IncentiveModuleApi;
import com.greenmobility.modules.incentive.dto.IncentiveDtos;
import com.greenmobility.modules.incentive.service.EcoStoreService;
import com.greenmobility.modules.incentive.service.IncentiveWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Incentives & Carbon Wallet", description = "APIs for PCC/Eco-Point Wallets, Eco-Store Redemptions, Voucher Preview, and PCC Conversion")
@RestController
@RequestMapping("/incentive")
public class IncentiveController {

    private final IncentiveWalletService walletService;
    private final EcoStoreService ecoStoreService;
    private final IdempotencyService idempotencyService;

    public IncentiveController(IncentiveWalletService walletService,
                               EcoStoreService ecoStoreService,
                               IdempotencyService idempotencyService) {
        this.walletService = walletService;
        this.ecoStoreService = ecoStoreService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "Xem tổng quan Ví Carbon (PCC) & Điểm Xanh (Eco-Points)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<IncentiveDtos.IncentiveSummaryDto>> getSummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.ok("Tổng quan ví khuyến khích", walletService.getSummary(currentUser.getId())));
    }

    @Operation(summary = "Lịch sử biến động PCC & Điểm Xanh phân trang cursor")
    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<IncentiveDtos.LedgerPageResponse>> getLedgerHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Lịch sử sổ cái khuyến khích",
                walletService.getLedgerHistory(currentUser.getId(), currency, type, from, to, cursor, limit)));
    }

    @Operation(summary = "Danh mục quà tặng & voucher Eco-Store")
    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<Page<IncentiveDtos.RewardCatalogDto>>> getCatalog(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Danh mục Eco-Store",
                ecoStoreService.listCatalog(true, category, page, size)));
    }

    @Operation(summary = "Xem danh sách quà đã đổi / voucher cá nhân")
    @GetMapping("/redemptions")
    public ResponseEntity<ApiResponse<Page<IncentiveDtos.RedemptionResponseDto>>> getMyRedemptions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Danh sách ưu đãi cá nhân",
                ecoStoreService.getMyRedemptions(currentUser.getId(), status, page, size)));
    }

    @Operation(summary = "Xem trước mức giảm giá voucher theo cước phí ước tính")
    @GetMapping("/vouchers/preview")
    public ResponseEntity<ApiResponse<IncentiveModuleApi.VoucherPreviewDto>> previewVoucher(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) UUID voucherRedemptionId,
            @RequestParam(required = false) String voucherCode,
            @RequestParam BigDecimal grossFareVnd) {
        return ResponseEntity.ok(ApiResponse.ok("Xem trước giảm giá voucher",
                ecoStoreService.previewVoucher(currentUser.getId(), voucherRedemptionId, voucherCode, grossFareVnd)));
    }

    @Operation(summary = "Đổi quà Eco-Store bằng PCC / Eco-Points (bắt buộc Idempotency-Key)")
    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<IncentiveDtos.RedemptionResponseDto>> redeemReward(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody IncentiveDtos.RedeemRewardRequest request) {
        return idempotencyService.executeWithIdempotency(
                idempotencyKey,
                currentUser.getId(),
                "/incentive/redeem",
                request,
                (Class<ApiResponse<IncentiveDtos.RedemptionResponseDto>>) (Class<?>) ApiResponse.class,
                true,
                () -> ResponseEntity.ok(ApiResponse.ok("Đổi ưu đãi thành công",
                        ecoStoreService.redeemReward(currentUser.getId(), request.rewardId(), idempotencyKey)))
        );
    }

    @Operation(summary = "Hủy quà đã đổi chưa dùng để hoàn PCC/Điểm Xanh")
    @PostMapping("/redemptions/{redemptionId}/cancel")
    public ResponseEntity<ApiResponse<IncentiveDtos.RedemptionResponseDto>> cancelRedemption(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID redemptionId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.ok("Hủy ưu đãi thành công",
                ecoStoreService.cancelRedemption(currentUser.getId(), redemptionId, reason)));
    }

    @Operation(summary = "Quy đổi Tín chỉ Carbon (PCC) sang VNĐ (bắt buộc Idempotency-Key)")
    @PostMapping("/carbon/convert")
    public ResponseEntity<ApiResponse<IncentiveDtos.ConvertPccResponse>> convertPccToVnd(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody IncentiveDtos.ConvertPccRequest request) {
        String role = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_CUSTOMER");
        return idempotencyService.executeWithIdempotency(
                idempotencyKey,
                currentUser.getId(),
                "/incentive/carbon/convert",
                request,
                (Class<ApiResponse<IncentiveDtos.ConvertPccResponse>>) (Class<?>) ApiResponse.class,
                true,
                () -> ResponseEntity.ok(ApiResponse.ok("Quy đổi Tín chỉ Carbon thành công",
                        walletService.convertPccToVnd(currentUser.getId(), role, request.pccAmount(), idempotencyKey)))
        );
    }
}
