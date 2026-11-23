package com.greenmobility.modules.fraud.dto;

import com.greenmobility.modules.fraud.entity.FraudAlert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FraudAlertDto {
    private UUID id;
    private UUID tripId;
    private UUID driverId;
    private UUID driverUserId;
    private UUID customerId;
    private String driverName;
    private String driverPhone;
    private String vehiclePlate;
    private String alertType;
    private BigDecimal riskScore;
    private String details;
    private String resolutionStatus;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private Instant createdAt;

    public FraudAlertDto() {}

    public static FraudAlertDto fromEntity(FraudAlert entity, String driverName, String driverPhone, String vehiclePlate) {
        return fromEntity(entity, null, driverName, driverPhone, vehiclePlate);
    }

    public static FraudAlertDto fromEntity(FraudAlert entity, UUID driverUserId, String driverName, String driverPhone, String vehiclePlate) {
        FraudAlertDto dto = new FraudAlertDto();
        dto.setId(entity.getId());
        dto.setTripId(entity.getTripId());
        dto.setDriverId(entity.getDriverId());
        dto.setDriverUserId(driverUserId);
        dto.setCustomerId(entity.getCustomerId());
        dto.setDriverName(driverName != null ? driverName : "Hệ thống");
        dto.setDriverPhone(driverPhone != null ? driverPhone : "-");
        dto.setVehiclePlate(vehiclePlate != null ? vehiclePlate : "-");
        dto.setAlertType(entity.getAlertType());
        dto.setRiskScore(entity.getRiskScore());
        dto.setDetails(entity.getDetails());
        dto.setResolutionStatus(entity.getResolutionStatus());
        dto.setResolvedBy(entity.getResolvedBy());
        dto.setResolvedAt(entity.getResolvedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public UUID getDriverUserId() { return driverUserId; }
    public void setDriverUserId(UUID driverUserId) { this.driverUserId = driverUserId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getResolutionStatus() { return resolutionStatus; }
    public void setResolutionStatus(String resolutionStatus) { this.resolutionStatus = resolutionStatus; }

    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
