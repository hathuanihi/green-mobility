package com.greenmobility.modules.trip.dto;

import java.time.Instant;
import java.util.UUID;

public class DriverArriveResponseDto {

    private UUID tripId;
    private String status;
    private Instant arrivedAt;
    private Integer noShowTimeoutSeconds;
    private String customerName;
    private String customerPhone;

    public DriverArriveResponseDto() {}

    public DriverArriveResponseDto(UUID tripId, String status, Instant arrivedAt, Integer noShowTimeoutSeconds, String customerName, String customerPhone) {
        this.tripId = tripId;
        this.status = status;
        this.arrivedAt = arrivedAt;
        this.noShowTimeoutSeconds = noShowTimeoutSeconds;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
    }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Instant arrivedAt) { this.arrivedAt = arrivedAt; }

    public Integer getNoShowTimeoutSeconds() { return noShowTimeoutSeconds; }
    public void setNoShowTimeoutSeconds(Integer noShowTimeoutSeconds) { this.noShowTimeoutSeconds = noShowTimeoutSeconds; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
}
