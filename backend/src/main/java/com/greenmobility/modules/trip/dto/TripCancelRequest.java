package com.greenmobility.modules.trip.dto;

public class TripCancelRequest {

    private String cancelReason;

    public TripCancelRequest() {}

    public TripCancelRequest(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
