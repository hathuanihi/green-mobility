package com.greenmobility.modules.drivervehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class VehicleResponse {

    private UUID id;
    private String vehicleType;
    private String make;
    private String model;
    private String licensePlate;
    private String color;
    private BigDecimal batteryCapacityKwh;
    private Integer rangePerChargeKm;
    private String registrationCertificateUrl;
    private LocalDate inspectionExpiryDate;
    private Boolean isVerified;

    public VehicleResponse() {}

    public VehicleResponse(UUID id, String vehicleType, String make, String model, String licensePlate,
                           String color, BigDecimal batteryCapacityKwh, Integer rangePerChargeKm,
                           String registrationCertificateUrl, LocalDate inspectionExpiryDate, Boolean isVerified) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.make = make;
        this.model = model;
        this.licensePlate = licensePlate;
        this.color = color;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.rangePerChargeKm = rangePerChargeKm;
        this.registrationCertificateUrl = registrationCertificateUrl;
        this.inspectionExpiryDate = inspectionExpiryDate;
        this.isVerified = isVerified;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

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
    public void setIsVerified(Boolean verified) { isVerified = verified; }
}
