package com.greenmobility.modules.payment.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.payment.dto.CheckoutResponse;
import com.greenmobility.modules.payment.dto.PaymentDto;
import com.greenmobility.modules.payment.dto.PaymentMethodDto;
import com.greenmobility.modules.payment.service.IdempotencyService;
import com.greenmobility.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Payment Operations", description = "APIs for trip payment, checkout, and status polling")
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public PaymentController(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "Lấy danh sách phương thức thanh toán khả dụng")
    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<List<PaymentMethodDto>>> getAvailableMethods(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<PaymentMethodDto> methods = paymentService.getAvailableMethods(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Danh sách phương thức thanh toán", methods));
    }

    @Operation(summary = "Lấy thông tin thanh toán của chuyến đi")
    @GetMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentByTrip(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId) {
        PaymentDto payment = paymentService.getPaymentByTripId(currentUser.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.ok(payment));
    }

    @Operation(summary = "Tạo phiên thanh toán cho chuyến đi (Checkout)")
    @PostMapping("/trips/{tripId}/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String method = body.getOrDefault("method", "CASH");
        String clientIp = request.getRemoteAddr();

        return idempotencyService.executeWithIdempotency(
                idempotencyKey,
                currentUser.getId(),
                "/payments/trips/" + tripId + "/checkout",
                body,
                (Class<ApiResponse<CheckoutResponse>>) (Class<?>) ApiResponse.class,
                () -> {
                    CheckoutResponse response = paymentService.createCheckout(currentUser.getId(), tripId, method, clientIp);
                    return ResponseEntity.ok(ApiResponse.ok("Khởi tạo thanh toán thành công", response));
                }
        );
    }

    @Operation(summary = "Kiểm tra trạng thái thanh toán (Polling)")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID paymentId) {
        String role = currentUser.getAuthorities().stream().findFirst().map(Object::toString).orElse("ROLE_CUSTOMER");
        PaymentDto payment = paymentService.getPaymentById(currentUser.getId(), role, paymentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .body(ApiResponse.ok(payment));
    }

    @Operation(summary = "Hủy phiên thanh toán online đang chờ")
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID paymentId) {
        paymentService.cancelPayment(currentUser.getId(), paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Hủy thanh toán thành công", null));
    }
}

