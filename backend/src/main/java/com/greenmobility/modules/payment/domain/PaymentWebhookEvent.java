package com.greenmobility.modules.payment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "gateway", nullable = false, length = 20)
    private String gateway;

    @Column(name = "gateway_txn_ref", length = 64)
    private String gatewayTxnRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid = false;

    @Column(name = "processing_result", nullable = false, length = 30)
    private String processingResult;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    public PaymentWebhookEvent() {}

    public PaymentWebhookEvent(String gateway, String gatewayTxnRef, String rawPayload,
                               boolean signatureValid, String processingResult, String sourceIp) {
        this.gateway = gateway;
        this.gatewayTxnRef = gatewayTxnRef;
        this.rawPayload = rawPayload;
        this.signatureValid = signatureValid;
        this.processingResult = processingResult;
        this.sourceIp = sourceIp;
        this.receivedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getGatewayTxnRef() { return gatewayTxnRef; }
    public void setGatewayTxnRef(String gatewayTxnRef) { this.gatewayTxnRef = gatewayTxnRef; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public boolean isSignatureValid() { return signatureValid; }
    public void setSignatureValid(boolean signatureValid) { this.signatureValid = signatureValid; }

    public String getProcessingResult() { return processingResult; }
    public void setProcessingResult(String processingResult) { this.processingResult = processingResult; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
}

