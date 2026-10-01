package com.greenmobility.modules.trip.dto;

import java.util.UUID;

public class DriverArrivingResponseDto {

    private UUID tripId;
    private String status;
    private RoutingResultDto routing;
    private LocationInfo pickup;
    private CustomerInfo customer;

    public DriverArrivingResponseDto() {}

    public DriverArrivingResponseDto(UUID tripId, String status, RoutingResultDto routing, LocationInfo pickup, CustomerInfo customer) {
        this.tripId = tripId;
        this.status = status;
        this.routing = routing;
        this.pickup = pickup;
        this.customer = customer;
    }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public RoutingResultDto getRouting() { return routing; }
    public void setRouting(RoutingResultDto routing) { this.routing = routing; }

    public LocationInfo getPickup() { return pickup; }
    public void setPickup(LocationInfo pickup) { this.pickup = pickup; }

    public CustomerInfo getCustomer() { return customer; }
    public void setCustomer(CustomerInfo customer) { this.customer = customer; }

    public static class LocationInfo {
        private String address;
        private Double lat;
        private Double lng;

        public LocationInfo() {}
        public LocationInfo(String address, Double lat, Double lng) {
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
    }

    public static class CustomerInfo {
        private String fullName;
        private String phoneNumber;

        public CustomerInfo() {}
        public CustomerInfo(String fullName, String phoneNumber) {
            this.fullName = fullName;
            this.phoneNumber = phoneNumber;
        }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }
}
