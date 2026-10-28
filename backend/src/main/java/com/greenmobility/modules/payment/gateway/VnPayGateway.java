package com.greenmobility.modules.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class VnPayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(VnPayGateway.class);
    private static final ZoneId GMT7_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(GMT7_ZONE);

    private final GatewayProperties properties;

    public VnPayGateway(GatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getGatewayName() {
        return "VNPAY";
    }

    @Override
    public CheckoutResult createCheckout(CheckoutRequest request) {
        GatewayProperties.VnPay config = properties.getVnpay();
        Instant now = Instant.now();
        Instant expiresAt = request.expiresAt() != null ? request.expiresAt() : now.plusSeconds(900); // 15 mins

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", config.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(request.amountVnd() * 100)); // VNPay uses * 100
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", request.gatewayTxnRef());
        vnpParams.put("vnp_OrderInfo", sanitizeOrderInfo(request.orderInfo()));
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", request.returnUrl() != null ? request.returnUrl() : properties.getPublicBaseUrl() + "/api/v1/payments/vnpay/return");
        vnpParams.put("vnp_IpAddr", request.clientIp() != null ? request.clientIp() : "127.0.0.1");
        vnpParams.put("vnp_CreateDate", VNP_DATE_FORMATTER.format(now));
        vnpParams.put("vnp_ExpireDate", VNP_DATE_FORMATTER.format(expiresAt));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext(); ) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String secureHash = HmacUtil.hmacSha512(config.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = config.getUrl() + "?" + query.toString();
        return new CheckoutResult(request.gatewayTxnRef(), paymentUrl, null, null, expiresAt);
    }

    @Override
    public IpnResult verifyIpn(Map<String, String> params, String rawBody) {
        GatewayProperties.VnPay config = properties.getVnpay();
        String receivedHash = params.get("vnp_SecureHash");

        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType")) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext(); ) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        String computedHash = HmacUtil.hmacSha512(config.getHashSecret(), hashData.toString());
        boolean isValidSignature = HmacUtil.constantTimeEquals(computedHash, receivedHash);

        String txnRef = params.get("vnp_TxnRef");
        long amountVnd = 0;
        try {
            amountVnd = Long.parseLong(params.getOrDefault("vnp_Amount", "0")) / 100;
        } catch (NumberFormatException ignored) {}

        String responseCode = params.getOrDefault("vnp_ResponseCode", "99");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "99");
        String gatewayTxnNo = params.get("vnp_TransactionNo");

        boolean isSuccess = isValidSignature && "00".equals(responseCode) && "00".equals(transactionStatus);

        return new IpnResult(
                isValidSignature,
                isSuccess,
                txnRef,
                amountVnd,
                gatewayTxnNo,
                responseCode,
                isSuccess ? "Confirm Success" : "Payment Failed"
        );
    }

    @Override
    public GatewayQueryResult queryTransaction(String gatewayTxnRef) {
        // In sandbox, return query state or default
        return new GatewayQueryResult(true, false, 0, null, "QUERY_SIMULATED");
    }

    private String sanitizeOrderInfo(String info) {
        if (info == null) return "Thanh toan Green Mobility";
        return info.replaceAll("[^a-zA-Z0-9 ]", " ").trim();
    }
}

