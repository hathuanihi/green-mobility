package com.greenmobility.modules.payment.gateway;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@Profile("!prod")
public class MockGateway implements PaymentGateway {

    private final GatewayProperties properties;

    public MockGateway(GatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getGatewayName() {
        return "MOCK";
    }

    @Override
    public CheckoutResult createCheckout(CheckoutRequest request) {
        Instant expiresAt = request.expiresAt() != null ? request.expiresAt() : Instant.now().plusSeconds(900);
        String mockCheckoutUrl = properties.getPublicBaseUrl() + "/api/v1/dev/payments/mock-checkout?ref=" +
                request.gatewayTxnRef() + "&amount=" + request.amountVnd();
        return new CheckoutResult(request.gatewayTxnRef(), mockCheckoutUrl, null, null, expiresAt);
    }

    @Override
    public IpnResult verifyIpn(Map<String, String> params, String rawBody) {
        String ref = params.get("ref");
        long amount = 0;
        try {
            amount = Long.parseLong(params.getOrDefault("amount", "0"));
        } catch (Exception ignored) {}

        boolean success = "00".equals(params.getOrDefault("code", "00"));
        return new IpnResult(true, success, ref, amount, "MOCK-TXN-" + ref, "00", "Mock success");
    }

    @Override
    public GatewayQueryResult queryTransaction(String gatewayTxnRef) {
        return new GatewayQueryResult(true, true, 100000, "MOCK-TXN-" + gatewayTxnRef, "SUCCESS");
    }
}

