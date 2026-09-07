package com.greenmobility.modules.drivervehicle.dto;

import jakarta.validation.constraints.NotBlank;

public class KycRejectRequest {

    @NotBlank(message = "Lý do từ chối hồ sơ không được để trống")
    private String rejectionReason;

    public KycRejectRequest() {}

    public KycRejectRequest(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
