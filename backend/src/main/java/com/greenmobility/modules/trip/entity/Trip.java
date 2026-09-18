package com.greenmobility.modules.trip.entity;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trip_code", unique = true, nullable = false, length = 32)
    private String tripCode;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    @Column(name = "pickup_geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point pickupGeom;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "dropoff_geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point dropoffGeom;

    @Column(name = "dropoff_address", nullable = false, columnDefinition = "TEXT")
    private String dropoffAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TripStatus status = TripStatus.REQUESTED;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancelled_by", length = 20)
    private String cancelledBy;

    @Column(name = "estimated_distance_m", nullable = false)
    private Integer estimatedDistanceM;

    @Column(name = "estimated_duration_s", nullable = false)
    private Integer estimatedDurationS;

    @Column(name = "actual_distance_m")
    private Integer actualDistanceM;

    @Column(name = "actual_duration_s")
    private Integer actualDurationS;

    @Column(name = "fare_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal fareAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "co2_saved_grams", precision = 10, scale = 2)
    private BigDecimal co2SavedGrams = BigDecimal.ZERO;

    @Column(name = "carbon_credits_earned", precision = 10, scale = 4)
    private BigDecimal carbonCreditsEarned = BigDecimal.ZERO;

    @Column(name = "loyalty_points_earned")
    private Integer loyaltyPointsEarned = 0;

    @Column(name = "requested_at")
    private Instant requestedAt = Instant.now();

    @Column(name = "matched_at")
    private Instant matchedAt;

    @Column(name = "arrived_pickup_at")
    private Instant arrivedPickupAt;

    @Column(name = "started_trip_at")
    private Instant startedTripAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public Trip() {}

    // Convenience coordinate getters
    public Double getPickupLat() {
        return pickupGeom != null ? pickupGeom.getY() : null;
    }

    public Double getPickupLng() {
        return pickupGeom != null ? pickupGeom.getX() : null;
    }

    public Double getDropoffLat() {
        return dropoffGeom != null ? dropoffGeom.getY() : null;
    }

    public Double getDropoffLng() {
        return dropoffGeom != null ? dropoffGeom.getX() : null;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Point getPickupGeom() {
        return pickupGeom;
    }

    public void setPickupGeom(Point pickupGeom) {
        this.pickupGeom = pickupGeom;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Point getDropoffGeom() {
        return dropoffGeom;
    }

    public void setDropoffGeom(Point dropoffGeom) {
        this.dropoffGeom = dropoffGeom;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Integer getEstimatedDistanceM() {
        return estimatedDistanceM;
    }

    public void setEstimatedDistanceM(Integer estimatedDistanceM) {
        this.estimatedDistanceM = estimatedDistanceM;
    }

    public Integer getEstimatedDurationS() {
        return estimatedDurationS;
    }

    public void setEstimatedDurationS(Integer estimatedDurationS) {
        this.estimatedDurationS = estimatedDurationS;
    }

    public Integer getActualDistanceM() {
        return actualDistanceM;
    }

    public void setActualDistanceM(Integer actualDistanceM) {
        this.actualDistanceM = actualDistanceM;
    }

    public Integer getActualDurationS() {
        return actualDurationS;
    }

    public void setActualDurationS(Integer actualDurationS) {
        this.actualDurationS = actualDurationS;
    }

    public BigDecimal getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getCo2SavedGrams() {
        return co2SavedGrams;
    }

    public void setCo2SavedGrams(BigDecimal co2SavedGrams) {
        this.co2SavedGrams = co2SavedGrams;
    }

    public BigDecimal getCarbonCreditsEarned() {
        return carbonCreditsEarned;
    }

    public void setCarbonCreditsEarned(BigDecimal carbonCreditsEarned) {
        this.carbonCreditsEarned = carbonCreditsEarned;
    }

    public Integer getLoyaltyPointsEarned() {
        return loyaltyPointsEarned;
    }

    public void setLoyaltyPointsEarned(Integer loyaltyPointsEarned) {
        this.loyaltyPointsEarned = loyaltyPointsEarned;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(Instant matchedAt) {
        this.matchedAt = matchedAt;
    }

    public Instant getArrivedPickupAt() {
        return arrivedPickupAt;
    }

    public void setArrivedPickupAt(Instant arrivedPickupAt) {
        this.arrivedPickupAt = arrivedPickupAt;
    }

    public Instant getStartedTripAt() {
        return startedTripAt;
    }

    public void setStartedTripAt(Instant startedTripAt) {
        this.startedTripAt = startedTripAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
