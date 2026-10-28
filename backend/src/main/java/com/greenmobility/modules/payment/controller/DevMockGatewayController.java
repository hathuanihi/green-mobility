package com.greenmobility.modules.payment.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.domain.TopupRequest;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.repository.TopupRequestRepository;
import com.greenmobility.modules.payment.service.PaymentService;
import com.greenmobility.modules.payment.service.TopupService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Hidden
@Profile("!prod")
@RestController
@RequestMapping("/dev/payments")
public class DevMockGatewayController {

    private final PaymentService paymentService;
    private final TopupService topupService;
    private final PaymentRepository paymentRepository;
    private final TopupRequestRepository topupRepository;

    public DevMockGatewayController(PaymentService paymentService,
                                    TopupService topupService,
                                    PaymentRepository paymentRepository,
                                    TopupRequestRepository topupRepository) {
        this.paymentService = paymentService;
        this.topupService = topupService;
        this.paymentRepository = paymentRepository;
        this.topupRepository = topupRepository;
    }

    @GetMapping(value = "/mock-checkout", produces = MediaType.TEXT_HTML_VALUE)
    public String renderMockCheckoutPage(@RequestParam String ref, @RequestParam(defaultValue = "0") long amount) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Mock Payment Gateway - Green Mobility</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                        .card { background: #1e293b; padding: 2rem; border-radius: 1rem; width: 380px; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.5); text-align: center; border: 1px solid #334155; }
                        h2 { color: #10b981; margin-top: 0; }
                        .amount { font-size: 1.75rem; font-weight: bold; margin: 1rem 0; color: #38bdf8; }
                        .ref { font-size: 0.875rem; color: #94a3b8; margin-bottom: 2rem; }
                        .btn { width: 100%; padding: 0.75rem; margin-bottom: 0.75rem; border: none; border-radius: 0.5rem; font-size: 1rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
                        .btn-success { background: #10b981; color: white; }
                        .btn-success:hover { background: #059669; }
                        .btn-fail { background: #ef4444; color: white; }
                        .btn-fail:hover { background: #dc2626; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>🌱 Cổng Giả Lập Mock Gateway</h2>
                        <div class="amount">%,d VNĐ</div>
                        <div class="ref">Mã giao dịch: %s</div>
                        <form method="POST" action="/api/v1/dev/payments/mock-simulate">
                            <input type="hidden" name="ref" value="%s" />
                            <input type="hidden" name="status" value="SUCCESS" />
                            <button type="submit" class="btn btn-success">✅ Xác nhận Thanh toán Thành công</button>
                        </form>
                        <form method="POST" action="/api/v1/dev/payments/mock-simulate">
                            <input type="hidden" name="ref" value="%s" />
                            <input type="hidden" name="status" value="FAILED" />
                            <button type="submit" class="btn btn-fail">❌ Giả lập Thanh toán Thất bại</button>
                        </form>
                    </div>
                </body>
                </html>
                """.formatted(amount, ref, ref, ref);
    }

    @PostMapping(value = "/mock-simulate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public String simulatePaymentResult(@RequestParam String ref, @RequestParam String status) {
        boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);

        if (ref.startsWith("TOPUP")) {
            Optional<TopupRequest> topupOpt = topupRepository.findByGatewayTxnRef(ref);
            if (topupOpt.isPresent()) {
                if (isSuccess) {
                    topupService.settleTopup(ref, "MOCK-TXN-" + ref);
                } else {
                    TopupRequest t = topupOpt.get();
                    t.setStatus("FAILED");
                    topupRepository.save(t);
                }
            }
        } else {
            Optional<Payment> paymentOpt = paymentRepository.findAll().stream()
                    .filter(p -> ref.equals(p.getGatewayTxnRef()))
                    .findFirst();

            if (paymentOpt.isPresent()) {
                Payment p = paymentOpt.get();
                if (isSuccess) {
                    paymentService.settle(p.getId(), "DEV_MOCK", "MOCK-TXN-" + ref);
                } else {
                    p.setStatus("FAILED");
                    p.setFailureCode("MOCK_USER_CANCEL");
                    p.setFailureMessage("Người dùng hủy thanh toán trên cổng Mock");
                    paymentRepository.save(p);
                }
            }
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8"><title>Kết quả</title>
                    <style>body{font-family:sans-serif;background:#0f172a;color:#f8fafc;text-align:center;padding:4rem;}</style>
                </head>
                <body>
                    <h2>%s</h2>
                    <p>Mã tham chiếu: %s. Bạn có thể đóng tab này và quay lại ứng dụng.</p>
                </body>
                </html>
                """.formatted(isSuccess ? "🎉 Thanh toán Thành công!" : "⚠️ Thanh toán Thất bại!", ref);
    }

    @PostMapping("/mock-trigger")
    public ResponseEntity<ApiResponse<String>> triggerMockDirectly(@RequestParam String ref, @RequestParam(defaultValue = "true") boolean success) {
        simulatePaymentResult(ref, success ? "SUCCESS" : "FAILED");
        return ResponseEntity.ok(ApiResponse.ok("Đã kích hoạt callback mock thành công", ref));
    }
}

