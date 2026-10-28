package com.greenmobility.modules.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.domain.PaymentWebhookEvent;
import com.greenmobility.modules.payment.domain.TopupRequest;
import com.greenmobility.modules.payment.gateway.*;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.repository.PaymentWebhookEventRepository;
import com.greenmobility.modules.payment.repository.TopupRequestRepository;
import com.greenmobility.modules.payment.service.PaymentService;
import com.greenmobility.modules.payment.service.TopupService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping
public class GatewayCallbackController {

    private static final Logger log = LoggerFactory.getLogger(GatewayCallbackController.class);

    private final VnPayGateway vnPayGateway;
    private final MomoGateway momoGateway;
    private final PaymentService paymentService;
    private final TopupService topupService;
    private final PaymentRepository paymentRepository;
    private final TopupRequestRepository topupRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final GatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper;

    public GatewayCallbackController(VnPayGateway vnPayGateway,
                                     MomoGateway momoGateway,
                                     PaymentService paymentService,
                                     TopupService topupService,
                                     PaymentRepository paymentRepository,
                                     TopupRequestRepository topupRepository,
                                     PaymentWebhookEventRepository webhookEventRepository,
                                     GatewayProperties gatewayProperties,
                                     ObjectMapper objectMapper) {
        this.vnPayGateway = vnPayGateway;
        this.momoGateway = momoGateway;
        this.paymentService = paymentService;
        this.topupService = topupService;
        this.paymentRepository = paymentRepository;
        this.topupRepository = topupRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.gatewayProperties = gatewayProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * VNPay Server-to-Server IPN (Public endpoint, strictly verified).
     */
    @GetMapping(value = "/payments/vnpay/ipn", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> handleVnPayIpn(@RequestParam Map<String, String> params, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        IpnResult result = vnPayGateway.verifyIpn(params, null);

        String payloadJson = "";
        try {
            payloadJson = objectMapper.writeValueAsString(params);
        } catch (Exception ignored) {}

        if (!result.isValidSignature()) {
            recordWebhook("VNPAY", result.gatewayTxnRef(), payloadJson, false, "REJECTED_SIGNATURE", clientIp);
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }

        String txnRef = result.gatewayTxnRef();
        if (txnRef == null) {
            recordWebhook("VNPAY", null, payloadJson, true, "UNKNOWN_ORDER", clientIp);
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        // Check if topup or trip payment
        if (txnRef.startsWith("TOPUP")) {
            Optional<TopupRequest> topupOpt = topupRepository.findByGatewayTxnRef(txnRef);
            if (topupOpt.isEmpty()) {
                recordWebhook("VNPAY", txnRef, payloadJson, true, "UNKNOWN_ORDER", clientIp);
                return Map.of("RspCode", "01", "Message", "Order not found");
            }
            TopupRequest topup = topupOpt.get();
            if (topup.getAmountVnd() != result.amountVnd()) {
                recordWebhook("VNPAY", txnRef, payloadJson, true, "REJECTED_AMOUNT", clientIp);
                return Map.of("RspCode", "04", "Message", "Invalid amount");
            }
            if ("PAID".equals(topup.getStatus())) {
                recordWebhook("VNPAY", txnRef, payloadJson, true, "DUPLICATE", clientIp);
                return Map.of("RspCode", "02", "Message", "Order already confirmed");
            }

            if (result.isSuccess()) {
                topupService.settleTopup(txnRef, result.gatewayTransactionNo());
                recordWebhook("VNPAY", txnRef, payloadJson, true, "APPLIED", clientIp);
                return Map.of("RspCode", "00", "Message", "Confirm Success");
            } else {
                topup.setStatus("FAILED");
                topupRepository.save(topup);
                recordWebhook("VNPAY", txnRef, payloadJson, true, "APPLIED", clientIp);
                return Map.of("RspCode", "00", "Message", "Confirm Success");
            }
        }

        // Trip payment
        Optional<Payment> paymentOpt = paymentRepository.findByGatewayAndGatewayTxnRef("VNPAY", txnRef);
        if (paymentOpt.isEmpty()) {
            recordWebhook("VNPAY", txnRef, payloadJson, true, "UNKNOWN_ORDER", clientIp);
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        Payment payment = paymentOpt.get();
        if (payment.getAmountVnd() != result.amountVnd()) {
            recordWebhook("VNPAY", txnRef, payloadJson, true, "REJECTED_AMOUNT", clientIp);
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }

        if ("PAID".equals(payment.getStatus())) {
            recordWebhook("VNPAY", txnRef, payloadJson, true, "DUPLICATE", clientIp);
            return Map.of("RspCode", "02", "Message", "Order already confirmed");
        }

        if (result.isSuccess()) {
            paymentService.settle(payment.getId(), "VNPAY_IPN", result.gatewayTransactionNo());
            recordWebhook("VNPAY", txnRef, payloadJson, true, "APPLIED", clientIp);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } else {
            payment.setStatus("FAILED");
            payment.setFailureCode(result.responseCode());
            payment.setFailureMessage(result.message());
            paymentRepository.save(payment);
            recordWebhook("VNPAY", txnRef, payloadJson, true, "APPLIED", clientIp);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }
    }

    /**
     * VNPay Return URL (Does NOT change state; redirects 302 to mobile deep link or web return).
     */
    @GetMapping("/payments/vnpay/return")
    public ResponseEntity<Void> handleVnPayReturn(@RequestParam Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        UUID targetId = null;

        if (txnRef != null) {
            if (txnRef.startsWith("TOPUP")) {
                targetId = topupRepository.findByGatewayTxnRef(txnRef).map(TopupRequest::getId).orElse(null);
            } else {
                targetId = paymentRepository.findByGatewayAndGatewayTxnRef("VNPAY", txnRef).map(Payment::getId).orElse(null);
            }
        }

        String deepLink = gatewayProperties.getAppReturnScheme() +
                "?gateway=VNPAY&ref=" + (txnRef != null ? txnRef : "") +
                (targetId != null ? "&paymentId=" + targetId : "");

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(deepLink)).build();
    }

    /**
     * MoMo Server-to-Server IPN.
     */
    @PostMapping(value = "/payments/momo/ipn", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleMomoIpn(@RequestBody String rawBody, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        IpnResult result = momoGateway.verifyIpn(null, rawBody);

        if (!result.isValidSignature()) {
            recordWebhook("MOMO", result.gatewayTxnRef(), rawBody, false, "REJECTED_SIGNATURE", clientIp);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String txnRef = result.gatewayTxnRef();
        if (txnRef != null) {
            if (txnRef.startsWith("TOPUP")) {
                topupRepository.findByGatewayTxnRef(txnRef).ifPresent(topup -> {
                    if (result.isSuccess() && !"PAID".equals(topup.getStatus())) {
                        topupService.settleTopup(txnRef, result.gatewayTransactionNo());
                    }
                });
            } else {
                paymentRepository.findByGatewayAndGatewayTxnRef("MOMO", txnRef).ifPresent(payment -> {
                    if (result.isSuccess() && !"PAID".equals(payment.getStatus())) {
                        paymentService.settle(payment.getId(), "MOMO_IPN", result.gatewayTransactionNo());
                    }
                });
            }
        }

        recordWebhook("MOMO", txnRef, rawBody, true, "APPLIED", clientIp);
        return ResponseEntity.noContent().build();
    }

    /**
     * MoMo Return URL.
     */
    @GetMapping("/payments/momo/return")
    public ResponseEntity<Void> handleMomoReturn(@RequestParam Map<String, String> params) {
        String orderId = params.get("orderId");
        String deepLink = gatewayProperties.getAppReturnScheme() + "?gateway=MOMO&ref=" + (orderId != null ? orderId : "");
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(deepLink)).build();
    }

    private void recordWebhook(String gateway, String ref, String raw, boolean valid, String result, String ip) {
        PaymentWebhookEvent event = new PaymentWebhookEvent(gateway, ref, raw, valid, result, ip);
        webhookEventRepository.save(event);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
