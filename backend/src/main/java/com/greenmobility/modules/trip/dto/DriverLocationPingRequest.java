package com.greenmobility.modules.trip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DriverLocationPingRequest {

    @NotNull(message = "Vĩ độ không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
    private Double lat;

    @NotNull(message = "Kinh độ không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
    private Double lng;

    private Double speedKmh;
    private Double bearing;

    @Min(value = 0, message = "Phần trăm pin tối thiểu là 0%")
    @Max(value = 100, message = "Phần trăm pin tối đa là 100%")
    private Integer batteryPercent = 100;

    public DriverLocationPingRequest() {}

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getSpeedKmh() {
        return speedKmh;
    }

    public void setSpeedKmh(Double speedKmh) {
        this.speedKmh = speedKmh;
    }

    public Double getBearing() {
        return bearing;
    }

    public void setBearing(Double bearing) {
        this.bearing = bearing;
    }

    public Integer getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(Integer batteryPercent) {
        this.batteryPercent = batteryPercent;
    }
}
