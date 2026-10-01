package com.greenmobility.modules.trip.dto;

import java.util.UUID;

public class DriverCancelResponseDto {

    private UUID tripId;
    private String status;
    private String cancelledBy;
    private String cancelReason;
    private Boolean isPenalized;

    public DriverCancelResponseDto() {}

    public DriverCancelResponseDto(UUID tripId, String status, String cancelledBy, String cancelReason, Boolean isPenalized) {
        this.tripId = tripId;
        this.status = status;
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
        this.isPenalized = isPenalized;
    }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public Boolean getIsPenalized() { return isPenalized; }
    public void setIsPenalized(Boolean penalized) { isPenalized = penalized; }
}
