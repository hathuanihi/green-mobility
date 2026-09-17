package com.greenmobility.modules.drivervehicle.dto;

import java.util.UUID;

public class AdminKycActionResponse {

    private UUID driverId;
    private String kycStatus;

    public AdminKycActionResponse() {}

    public AdminKycActionResponse(UUID driverId, String kycStatus) {
        this.driverId = driverId;
        this.kycStatus = kycStatus;
    }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
}
