package com.greenmobility.modules.payment.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment")
public class GatewayProperties {

    private String gatewayMode = "sandbox"; // mock | sandbox
    private String publicBaseUrl = "http://localhost:8080";
    private String appReturnScheme = "greenmobility://payment/result";
    private String webReturnUrl = "http://localhost:3000/payment/result";

    private VnPay vnpay = new VnPay();
    private Momo momo = new Momo();

    public static class VnPay {
        private String tmnCode = "DEMOVNPAY";
        private String hashSecret = "DEMOHASHSECRET";
        private String url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

        public String getTmnCode() { return tmnCode; }
        public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }
        public String getHashSecret() { return hashSecret; }
        public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    }

    public static class Momo {
        private String partnerCode = "MOMO";
        private String accessKey = "DEMOACCESSKEY";
        private String secretKey = "DEMOSECRETKEY";
        private String endpoint = "https://test-payment.momo.vn";

        public String getPartnerCode() { return partnerCode; }
        public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }

    public String getGatewayMode() { return gatewayMode; }
    public void setGatewayMode(String gatewayMode) { this.gatewayMode = gatewayMode; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public String getAppReturnScheme() { return appReturnScheme; }
    public void setAppReturnScheme(String appReturnScheme) { this.appReturnScheme = appReturnScheme; }
    public String getWebReturnUrl() { return webReturnUrl; }
    public void setWebReturnUrl(String webReturnUrl) { this.webReturnUrl = webReturnUrl; }
    public VnPay getVnpay() { return vnpay; }
    public void setVnpay(VnPay vnpay) { this.vnpay = vnpay; }
    public Momo getMomo() { return momo; }
    public void setMomo(Momo momo) { this.momo = momo; }
}

