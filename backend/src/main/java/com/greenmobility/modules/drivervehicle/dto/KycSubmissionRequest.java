package com.greenmobility.modules.drivervehicle.dto;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

public class KycSubmissionRequest {

    private String citizenId;
    private String licenseNumber;
    private String licenseClass;
    private VehicleType vehicleType;
    private String make;
    private String model;
    private String licensePlate;
    private String color;
    private BigDecimal batteryCapacityKwh;
    private Integer rangePerChargeKm;
    private LocalDate inspectionExpiryDate;

    private MultipartFile citizenFrontImage;
    private MultipartFile citizenBackImage;
    private MultipartFile licenseImage;
    private MultipartFile vehicleRegistrationImage;
    private MultipartFile facePortraitImage;

    public KycSubmissionRequest() {}

    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getLicenseClass() { return licenseClass; }
    public void setLicenseClass(String licenseClass) { this.licenseClass = licenseClass; }

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

    public LocalDate getInspectionExpiryDate() { return inspectionExpiryDate; }
    public void setInspectionExpiryDate(LocalDate inspectionExpiryDate) { this.inspectionExpiryDate = inspectionExpiryDate; }

    public MultipartFile getCitizenFrontImage() { return citizenFrontImage; }
    public void setCitizenFrontImage(MultipartFile citizenFrontImage) { this.citizenFrontImage = citizenFrontImage; }

    public MultipartFile getCitizenBackImage() { return citizenBackImage; }
    public void setCitizenBackImage(MultipartFile citizenBackImage) { this.citizenBackImage = citizenBackImage; }

    public MultipartFile getLicenseImage() { return licenseImage; }
    public void setLicenseImage(MultipartFile licenseImage) { this.licenseImage = licenseImage; }

    public MultipartFile getVehicleRegistrationImage() { return vehicleRegistrationImage; }
    public void setVehicleRegistrationImage(MultipartFile vehicleRegistrationImage) { this.vehicleRegistrationImage = vehicleRegistrationImage; }

    public MultipartFile getFacePortraitImage() { return facePortraitImage; }
    public void setFacePortraitImage(MultipartFile facePortraitImage) { this.facePortraitImage = facePortraitImage; }
}
