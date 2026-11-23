package com.greenmobility.modules.incentive.controller;

import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.incentive.dto.IncentiveDtos;
import com.greenmobility.modules.incentive.service.EcoStoreService;
import com.greenmobility.modules.incentive.service.IncentiveAwardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin Incentive Governance", description = "APIs for Eco-Store Catalog, Held Fraud Awards Approval, and Incentive Settings")
@RestController
@RequestMapping("/admin/incentive")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_FINANCE', 'FINANCE', 'ROLE_OPERATOR', 'OPERATOR')")
public class AdminIncentiveController {

    private final EcoStoreService ecoStoreService;
    private final IncentiveAwardService awardService;
    private final PlatformSettingsService settingsService;
    private final AuditLogService auditLogService;

    public AdminIncentiveController(EcoStoreService ecoStoreService,
                                    IncentiveAwardService awardService,
                                    PlatformSettingsService settingsService,
                                    AuditLogService auditLogService) {
        this.ecoStoreService = ecoStoreService;
        this.awardService = awardService;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Danh sách danh mục quà Eco-Store (Admin)")
    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<Page<IncentiveDtos.RewardCatalogDto>>> getCatalog(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Danh mục Eco-Store",
                ecoStoreService.listCatalog(activeOnly, category, page, size)));
    }

    @Operation(summary = "Tạo mới quà/voucher Eco-Store")
    @PostMapping("/catalog")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_FINANCE', 'FINANCE')")
    public ResponseEntity<ApiResponse<IncentiveDtos.RewardCatalogDto>> createReward(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody IncentiveDtos.AdminRewardRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo ưu đãi Eco-Store thành công",
                ecoStoreService.createReward(currentUser.getId(), currentUser.getUsername(), req, httpReq.getRemoteAddr())));
    }

    @Operation(summary = "Cập nhật thông tin / tồn kho quà Eco-Store")
    @PutMapping("/catalog/{rewardId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_FINANCE', 'FINANCE')")
    public ResponseEntity<ApiResponse<IncentiveDtos.RewardCatalogDto>> updateReward(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID rewardId,
            @Valid @RequestBody IncentiveDtos.AdminRewardRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật ưu đãi thành công",
                ecoStoreService.updateReward(currentUser.getId(), currentUser.getUsername(), rewardId, req, httpReq.getRemoteAddr())));
    }

    @Operation(summary = "Tra cứu danh sách điều phối thưởng chuyến đi (Join-Barrier)")
    @GetMapping("/awards")
    public ResponseEntity<ApiResponse<Page<IncentiveDtos.TripAwardDto>>> listAwards(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Danh sách giải ngân thưởng", awardService.searchAwards(status, page, size)));
    }

    @Operation(summary = "Duyệt cộng thưởng cho chuyến đi bị giữ HELD_FOR_FRAUD")
    @PostMapping("/awards/{awardId}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_FINANCE', 'FINANCE')")
    public ResponseEntity<ApiResponse<IncentiveDtos.TripAwardDto>> approveHeldAward(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID awardId,
            @RequestBody(required = false) IncentiveDtos.AdminReviewAwardRequest req,
            HttpServletRequest httpReq) {
        String note = req != null ? req.note() : null;
        return ResponseEntity.ok(ApiResponse.ok("Duyệt giải ngân thưởng thành công",
                awardService.approveHeldAward(awardId, currentUser.getId(), currentUser.getUsername(), note, httpReq.getRemoteAddr())));
    }

    @Operation(summary = "Hủy vĩnh viễn thưởng cho chuyến đi HELD_FOR_FRAUD")
    @PostMapping("/awards/{awardId}/void")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_FINANCE', 'FINANCE')")
    public ResponseEntity<ApiResponse<IncentiveDtos.TripAwardDto>> voidHeldAward(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID awardId,
            @Valid @RequestBody IncentiveDtos.AdminReviewAwardRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy thưởng chuyến đi vi phạm",
                awardService.voidHeldAward(awardId, currentUser.getId(), currentUser.getUsername(), req.note(), httpReq.getRemoteAddr())));
    }

    @Operation(summary = "Lấy cấu hình tham số khuyến khích (tỷ lệ PCC, Eco-Points, quy đổi)")
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.ok("Cấu hình tham số khuyến khích", settingsService.getAllSettings()));
    }

    @Operation(summary = "Cập nhật tham số khuyến khích")
    @PutMapping("/settings")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_FINANCE', 'FINANCE')")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateSettings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody Map<String, String> updates,
            HttpServletRequest httpReq) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            settingsService.updateSetting(entry.getKey(), entry.getValue(), currentUser.getId());
        }
        auditLogService.record(currentUser.getId(), currentUser.getUsername(), "UPDATE_INCENTIVE_SETTINGS",
                "PlatformSetting", "incentive_settings", null, updates, httpReq.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật cấu hình khuyến khích thành công", settingsService.getAllSettings()));
    }
}
