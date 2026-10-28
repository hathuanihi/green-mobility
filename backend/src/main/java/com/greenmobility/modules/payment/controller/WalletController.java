package com.greenmobility.modules.payment.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.payment.dto.TopupDtos;
import com.greenmobility.modules.payment.dto.WalletDtos;
import com.greenmobility.modules.payment.service.IdempotencyService;
import com.greenmobility.modules.payment.service.TopupService;
import com.greenmobility.modules.payment.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Wallet Operations", description = "APIs for Green Wallet balance, transaction history and top-ups")
@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    private final TopupService topupService;
    private final IdempotencyService idempotencyService;

    public WalletController(WalletService walletService,
                            TopupService topupService,
                            IdempotencyService idempotencyService) {
        this.walletService = walletService;
        this.topupService = topupService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "Lấy thông tin số dư Ví Xanh")
    @GetMapping
    public ResponseEntity<ApiResponse<WalletDtos.WalletSummaryResponse>> getWallet(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        WalletDtos.WalletSummaryResponse summary = walletService.getWalletSummary(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Thông tin ví người dùng", summary));
    }

    @Operation(summary = "Lấy lịch sử biến động số dư Ví Xanh (Phân trang con trỏ Cursor)")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<WalletDtos.CursorPaginationResponse<WalletDtos.WalletTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {

        WalletDtos.CursorPaginationResponse<WalletDtos.WalletTransactionResponse> res =
                walletService.getTransactionHistory(currentUser.getId(), type, from, to, limit, cursor);
        return ResponseEntity.ok(ApiResponse.ok("Lịch sử giao dịch", res));
    }

    @Operation(summary = "Tạo yêu cầu nạp tiền vào Ví Xanh")
    @PostMapping("/topups")
    public ResponseEntity<ApiResponse<TopupDtos.TopupResponse>> createTopup(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody TopupDtos.CreateTopupRequest request) {

        return idempotencyService.executeWithIdempotency(
                idempotencyKey,
                currentUser.getId(),
                "/wallet/topups",
                request,
                (Class<ApiResponse<TopupDtos.TopupResponse>>) (Class<?>) ApiResponse.class,
                () -> {
                    TopupDtos.TopupResponse response = topupService.createTopup(
                            currentUser.getId(),
                            request.amountVnd(),
                            request.gateway()
                    );
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponse.ok("Tạo yêu cầu nạp tiền thành công", response));
                }
        );
    }

    @Operation(summary = "Kiểm tra trạng thái nạp tiền")
    @GetMapping("/topups/{id}")
    public ResponseEntity<ApiResponse<TopupDtos.TopupResponse>> getTopup(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        TopupDtos.TopupResponse res = topupService.getTopupById(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}

