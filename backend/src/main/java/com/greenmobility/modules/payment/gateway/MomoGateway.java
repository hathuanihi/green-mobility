package com.greenmobility.modules.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MomoGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MomoGateway.class);

    private final GatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MomoGateway(GatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String getGatewayName() {
        return "MOMO";
    }

    @Override
    public CheckoutResult createCheckout(CheckoutRequest request) {
        GatewayProperties.Momo config = properties.getMomo();
        String requestId = UUID.randomUUID().toString();
        String orderId = request.gatewayTxnRef();
        String amount = String.valueOf(request.amountVnd());
        String orderInfo = request.orderInfo() != null ? request.orderInfo() : "Thanh toan Green Mobility";
        String redirectUrl = request.returnUrl() != null ? request.returnUrl() : properties.getPublicBaseUrl() + "/api/v1/payments/momo/return";
        String ipnUrl = properties.getPublicBaseUrl() + "/api/v1/payments/momo/ipn";
        String requestType = "captureWallet";
        String extraData = "";

        // Sign according to exact MoMo order
        String rawSignature = "accessKey=" + config.getAccessKey() +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + config.getPartnerCode() +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = HmacUtil.hmacSha256(config.getSecretKey(), rawSignature);

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("partnerCode", config.getPartnerCode());
        reqBody.put("requestId", requestId);
        reqBody.put("amount", request.amountVnd());
        reqBody.put("orderId", orderId);
        reqBody.put("orderInfo", orderInfo);
        reqBody.put("redirectUrl", redirectUrl);
        reqBody.put("ipnUrl", ipnUrl);
        reqBody.put("lang", "vi");
        reqBody.put("extraData", extraData);
        reqBody.put("requestType", requestType);
        reqBody.put("signature", signature);

        Instant expiresAt = request.expiresAt() != null ? request.expiresAt() : Instant.now().plusSeconds(900);

        try {
            String response = restClient.post()
                    .uri(config.getEndpoint() + "/v2/gateway/api/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reqBody)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            int resultCode = node.path("resultCode").asInt(-1);
            if (resultCode == 0) {
                String payUrl = node.path("payUrl").asText();
                String deeplink = node.path("deeplink").asText(null);
                String qrCodeUrl = node.path("qrCodeUrl").asText(null);
                return new CheckoutResult(orderId, payUrl, deeplink, qrCodeUrl, expiresAt);
            } else {
                log.warn("MoMo createCheckout error resultCode={}, message={}", resultCode, node.path("message").asText());
            }
        } catch (Exception e) {
            log.warn("Failed to call MoMo online sandbox: {}. Falling back to simulated pay URL.", e.getMessage());
        }

        // Fallback simulated URL for offline test/sandbox
        String simulatedPayUrl = config.getEndpoint() + "/v2/gateway/pay?orderId=" + orderId + "&signature=" + signature;
        return new CheckoutResult(orderId, simulatedPayUrl, null, null, expiresAt);
    }

    @Override
    public IpnResult verifyIpn(Map<String, String> params, String rawBody) {
        GatewayProperties.Momo config = properties.getMomo();

        try {
            JsonNode node;
            if (rawBody != null && !rawBody.isBlank()) {
                node = objectMapper.readTree(rawBody);
            } else {
                node = objectMapper.valueToTree(params);
            }

            String accessKey = node.path("accessKey").asText();
            String amount = node.path("amount").asText();
            String extraData = node.path("extraData").asText();
            String message = node.path("message").asText();
            String orderId = node.path("orderId").asText();
            String orderInfo = node.path("orderInfo").asText();
            String orderType = node.path("orderType").asText();
            String partnerCode = node.path("partnerCode").asText();
            String payType = node.path("payType").asText();
            String requestId = node.path("requestId").asText();
            String responseTime = node.path("responseTime").asText();
            String resultCode = node.path("resultCode").asText();
            String transId = node.path("transId").asText();
            String receivedSignature = node.path("signature").asText();

            String rawSign = "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&extraData=" + extraData +
                    "&message=" + message +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&orderType=" + orderType +
                    "&partnerCode=" + partnerCode +
                    "&payType=" + payType +
                    "&requestId=" + requestId +
                    "&responseTime=" + responseTime +
                    "&resultCode=" + resultCode +
                    "&transId=" + transId;

            String computed = HmacUtil.hmacSha256(config.getSecretKey(), rawSign);
            boolean isValid = HmacUtil.constantTimeEquals(computed, receivedSignature);

            long amountVnd = 0;
            try {
                amountVnd = Long.parseLong(amount);
            } catch (NumberFormatException ignored) {}

            boolean isSuccess = isValid && "0".equals(resultCode);
            return new IpnResult(isValid, isSuccess, orderId, amountVnd, transId, resultCode, message);
        } catch (Exception e) {
            log.error("Failed to parse MoMo IPN: {}", e.getMessage());
            return new IpnResult(false, false, null, 0, null, "99", "Failed to parse IPN");
        }
    }

    @Override
    public GatewayQueryResult queryTransaction(String gatewayTxnRef) {
        return new GatewayQueryResult(true, false, 0, null, "QUERY_SIMULATED");
    }
}

