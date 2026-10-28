package com.greenmobility.modules.payment.gateway;

import java.util.Map;

public interface PaymentGateway {

    String getGatewayName();

    CheckoutResult createCheckout(CheckoutRequest request);

    IpnResult verifyIpn(Map<String, String> params, String rawBody);

    GatewayQueryResult queryTransaction(String gatewayTxnRef);
}

