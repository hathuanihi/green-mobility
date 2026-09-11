package com.greenmobility.modules.drivervehicle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", unique = true, nullable = false)
    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "license_plate", unique = true, nullable = false, length = 20)
    private String licensePlate;

    @Column(nullable = false, length = 30)
    private String color;

    @Column(name = "battery_capacity_kwh", nullable = false, precision = 5, scale = 2)
    private BigDecimal batteryCapacityKwh;

    @Column(name = "range_per_charge_km", nullable = false)
    private Integer rangePerChargeKm;

    @Column(name = "registration_certificate_url", length = 500)
    private String registrationCertificateUrl;

    @Column(name = "inspection_expiry_date", nullable = false)
    private LocalDate inspectionExpiryDate;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public Vehicle() {}

    public Vehicle(UUID driverId, VehicleType vehicleType, String make, String model, String licensePlate,
                   String color, BigDecimal batteryCapacityKwh, Integer rangePerChargeKm, LocalDate inspectionExpiryDate) {
        this.driverId = driverId;
        this.vehicleType = vehicleType;
        this.make = make;
        this.model = model;
        this.licensePlate = licensePlate;
        this.color = color;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.rangePerChargeKm = rangePerChargeKm;
        this.inspectionExpiryDate = inspectionExpiryDate;
        this.isVerified = false;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

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
    public void setIsVerified(Boolean verified) { isVerified = verified; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
