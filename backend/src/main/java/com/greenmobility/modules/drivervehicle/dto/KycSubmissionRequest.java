package com.greenmobility.modules.drivervehicle.dto;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

public class KycSubmissionRequest {

    @NotBlank(message = "Số CCCD không được để trống")
    private String citizenId;

    @NotBlank(message = "Số Giấy phép lái xe không được để trống")
    private String licenseNumber;

    @NotBlank(message = "Hạng Giấy phép lái xe không được để trống")
    private String licenseClass;

    @NotNull(message = "Loại phương tiện xe điện không được để trống")
    private VehicleType vehicleType;

    @NotBlank(message = "Hãng xe không được để trống")
    private String make;

    @NotBlank(message = "Dòng xe/Model không được để trống")
    private String model;

    @NotBlank(message = "Biển số xe không được để trống")
    private String licensePlate;

    @NotBlank(message = "Màu xe không được để trống")
    private String color;

    @NotNull(message = "Dung lượng pin không được để trống")
    @DecimalMin(value = "0.5", message = "Dung lượng pin thiết kế phải từ 0.5 kWh trở lên")
    private BigDecimal batteryCapacityKwh;

    @NotNull(message = "Quãng đường di chuyển 1 lần sạc không được để trống")
    @Min(value = 10, message = "Quãng đường di chuyển tối thiểu phải từ 10 km")
    private Integer rangePerChargeKm;

    @NotNull(message = "Hạn kiểm định không được để trống")
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
