package com.greenmobility.modules.drivervehicle.dto;

import com.greenmobility.modules.drivervehicle.entity.Vehicle;
import com.greenmobility.modules.drivervehicle.entity.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdminVehicleDto {

    private UUID id;
    private UUID driverId;
    private String driverName;
    private String driverPhone;
    private VehicleType vehicleType;
    private String make;
    private String model;
    private String licensePlate;
    private String color;
    private BigDecimal batteryCapacityKwh;
    private Integer rangePerChargeKm;
    private String registrationCertificateUrl;
    private LocalDate inspectionExpiryDate;
    private Boolean isVerified;
    private Instant createdAt;

    public static AdminVehicleDto fromEntity(Vehicle vehicle, String driverName, String driverPhone) {
        if (vehicle == null) return null;
        AdminVehicleDto dto = new AdminVehicleDto();
        dto.setId(vehicle.getId());
        dto.setDriverId(vehicle.getDriverId());
        dto.setDriverName(driverName);
        dto.setDriverPhone(driverPhone);
        dto.setVehicleType(vehicle.getVehicleType());
        dto.setMake(vehicle.getMake());
        dto.setModel(vehicle.getModel());
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setColor(vehicle.getColor());
        dto.setBatteryCapacityKwh(vehicle.getBatteryCapacityKwh());
        dto.setRangePerChargeKm(vehicle.getRangePerChargeKm());
        dto.setRegistrationCertificateUrl(vehicle.getRegistrationCertificateUrl());
        dto.setInspectionExpiryDate(vehicle.getInspectionExpiryDate());
        dto.setIsVerified(vehicle.getIsVerified());
        dto.setCreatedAt(vehicle.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public BigDecimal getBatteryCapacityKwh() { return batteryCapacityKwh; }
    public void setBatteryCapacityKwh(BigDecimal batteryCapacityKwh) { this.batteryCapacityKwh = batteryCapacityKwh; }

    public Integer getRangePerChargeKm() { return rangePerChargeKm; }
    public void setRangePerChargeKm(Integer rangePerChargeKm) { this.rangePerChargeKm = rangePerChargeKm; }

    public String getRegistrationCertificateUrl() { return registrationCertificateUrl; }
    public void setRegistrationCertificateUrl(String registrationCertificateUrl) { this.registrationCertificateUrl = registrationCertificateUrl; }

    public LocalDate getInspectionExpiryDate() { return inspectionExpiryDate; }
    public void setInspectionExpiryDate(LocalDate inspectionExpiryDate) { this.inspectionExpiryDate = inspectionExpiryDate; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
