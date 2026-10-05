package com.greenmobility.modules.trip.dto;

import java.math.BigDecimal;

/**
 * KPI stats for Today's operations.
 * Used by GET /admin/trips/stats/today
 */
public class TodayTripStatsDto {

    private long activeTripsCount;
    private double avgPickupTimeMinutes;
    private long avgPickupTimeSeconds;
    private double totalKmToday;
    private BigDecimal co2SavedTodayGrams;
    private double co2SavedTodayKg;
    private long completedTripsTodayCount;

    public TodayTripStatsDto() {}

    public TodayTripStatsDto(long activeTripsCount, double avgPickupTimeMinutes, long avgPickupTimeSeconds,
                             double totalKmToday, BigDecimal co2SavedTodayGrams, double co2SavedTodayKg,
                             long completedTripsTodayCount) {
        this.activeTripsCount = activeTripsCount;
        this.avgPickupTimeMinutes = avgPickupTimeMinutes;
        this.avgPickupTimeSeconds = avgPickupTimeSeconds;
        this.totalKmToday = totalKmToday;
        this.co2SavedTodayGrams = co2SavedTodayGrams;
        this.co2SavedTodayKg = co2SavedTodayKg;
        this.completedTripsTodayCount = completedTripsTodayCount;
    }

    public long getActiveTripsCount() { return activeTripsCount; }
    public void setActiveTripsCount(long activeTripsCount) { this.activeTripsCount = activeTripsCount; }

    public double getAvgPickupTimeMinutes() { return avgPickupTimeMinutes; }
    public void setAvgPickupTimeMinutes(double avgPickupTimeMinutes) { this.avgPickupTimeMinutes = avgPickupTimeMinutes; }

    public long getAvgPickupTimeSeconds() { return avgPickupTimeSeconds; }
    public void setAvgPickupTimeSeconds(long avgPickupTimeSeconds) { this.avgPickupTimeSeconds = avgPickupTimeSeconds; }

    public double getTotalKmToday() { return totalKmToday; }
    public void setTotalKmToday(double totalKmToday) { this.totalKmToday = totalKmToday; }

    public BigDecimal getCo2SavedTodayGrams() { return co2SavedTodayGrams; }
    public void setCo2SavedTodayGrams(BigDecimal co2SavedTodayGrams) { this.co2SavedTodayGrams = co2SavedTodayGrams; }

    public double getCo2SavedTodayKg() { return co2SavedTodayKg; }
    public void setCo2SavedTodayKg(double co2SavedTodayKg) { this.co2SavedTodayKg = co2SavedTodayKg; }

    public long getCompletedTripsTodayCount() { return completedTripsTodayCount; }
    public void setCompletedTripsTodayCount(long completedTripsTodayCount) { this.completedTripsTodayCount = completedTripsTodayCount; }
}
