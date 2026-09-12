package com.greenmobility.modules.drivervehicle.dto;

import java.time.Instant;
import java.util.UUID;

public class KycSubmitResponse {

    private UUID driverId;
    private String kycStatus;
    private Instant submittedAt;

    public KycSubmitResponse() {}

    public KycSubmitResponse(UUID driverId, String kycStatus, Instant submittedAt) {
        this.driverId = driverId;
        this.kycStatus = kycStatus;
        this.submittedAt = submittedAt;
    }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
