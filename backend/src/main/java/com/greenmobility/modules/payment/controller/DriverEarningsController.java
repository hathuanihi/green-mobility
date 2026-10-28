package com.greenmobility.modules.payment.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.payment.dto.DriverEarningsDtos;
import com.greenmobility.modules.payment.dto.WalletDtos;
import com.greenmobility.modules.payment.dto.WithdrawalDtos;
import com.greenmobility.modules.payment.service.DriverEarningsService;
import com.greenmobility.modules.payment.service.IdempotencyService;
import com.greenmobility.modules.payment.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Driver Financial Operations", description = "APIs for Driver Earnings, Cash Settlement, and Withdrawals")
@RestController
@RequestMapping("/driver")
public class DriverEarningsController {

    private final DriverEarningsService driverEarningsService;
    private final WithdrawalService withdrawalService;
    private final IdempotencyService idempotencyService;

    public DriverEarningsController(DriverEarningsService driverEarningsService,
                                    WithdrawalService withdrawalService,
                                    IdempotencyService idempotencyService) {
        this.driverEarningsService = driverEarningsService;
        this.withdrawalService = withdrawalService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "Lấy báo cáo tổng hợp thu nhập tài xế theo chu kỳ (Hôm nay, Tuần, Tháng)")
    @GetMapping("/earnings/summary")
    public ResponseEntity<ApiResponse<DriverEarningsDtos.EarningsSummaryResponse>> getSummary(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "WEEK") String range) {
        DriverEarningsDtos.EarningsSummaryResponse summary = driverEarningsService.getSummary(currentUser.getId(), range);
        return ResponseEntity.ok(ApiResponse.ok("Báo cáo thu nhập", summary));
    }

    @Operation(summary = "Lấy danh sách chi tiết thu nhập từng cuốc xe")
    @GetMapping("/earnings/trips")
    public ResponseEntity<ApiResponse<WalletDtos.CursorPaginationResponse<DriverEarningsDtos.DriverTripEarningResponse>>> getTrips(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        WalletDtos.CursorPaginationResponse<DriverEarningsDtos.DriverTripEarningResponse> res =
                driverEarningsService.getTrips(currentUser.getId(), limit, cursor);
        return ResponseEntity.ok(ApiResponse.ok("Lịch sử cuốc xe và thu nhập", res));
    }

    @Operation(summary = "Kiểm tra hạn mức nợ tiền mặt và điều kiện nhận cuốc tiền mặt")
    @GetMapping("/payment/eligibility")
    public ResponseEntity<ApiResponse<DriverEarningsDtos.CashEligibilityResponse>> getEligibility(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        DriverEarningsDtos.CashEligibilityResponse eligibility = driverEarningsService.getEligibility(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Điều kiện nhận cuốc tiền mặt", eligibility));
    }

    @Operation(summary = "Tài xế xác nhận đã thu tiền mặt từ khách hàng")
    @PostMapping("/trips/{tripId}/cash-collected")
    public ResponseEntity<ApiResponse<Void>> confirmCashCollected(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return idempotencyService.executeWithIdempotency(
                idempotencyKey,
                currentUser.getId(),
                "/driver/trips/" + tripId + "/cash-collected",
                "{}",
                (Class<ApiResponse<Void>>) (Class<?>) ApiResponse.class,
                () -> {
                    driverEarningsService.confirmCashCollected(currentUser.getId(), tripId);
                    return ResponseEntity.ok(ApiResponse.ok("Xác nhận thu tiền mặt thành công", null));
                }
        );
    }

    @Operation(summary = "Tạo yêu cầu rút thu nhập về tài khoản ngân hàng")
    @PostMapping("/withdrawals")
    public ResponseEntity<ApiResponse<WithdrawalDtos.WithdrawalResponse>> requestWithdrawal(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody WithdrawalDtos.CreateWithdrawalRequest request) {

        return idempotencyService.executeWithIdempotency(
                idempotencyKey,
                currentUser.getId(),
                "/driver/withdrawals",
                request,
                (Class<ApiResponse<WithdrawalDtos.WithdrawalResponse>>) (Class<?>) ApiResponse.class,
                () -> {
                    WithdrawalDtos.WithdrawalResponse res = withdrawalService.requestWithdrawal(
                            currentUser.getId(),
                            request.amountVnd(),
                            request.bankCode(),
                            request.accountNo(),
                            request.accountHolder()
                    );
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponse.ok("Tạo yêu cầu rút tiền thành công", res));
                }
        );
    }

    @Operation(summary = "Xem lịch sử các yêu cầu rút tiền của tài xế")
    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<List<WithdrawalDtos.WithdrawalResponse>>> getWithdrawals(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<WithdrawalDtos.WithdrawalResponse> list = withdrawalService.getDriverWithdrawals(currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok("Danh sách yêu cầu rút tiền", list));
    }

    @Operation(summary = "Tài xế tự hủy yêu cầu rút tiền đang chờ duyệt")
    @PostMapping("/withdrawals/{id}/cancel")
    public ResponseEntity<ApiResponse<WithdrawalDtos.WithdrawalResponse>> cancelWithdrawal(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        WithdrawalDtos.WithdrawalResponse res = withdrawalService.cancelWithdrawal(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Hủy yêu cầu rút tiền thành công", res));
    }
}

