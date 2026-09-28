package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculationService {

    // E-Bike: 12,000 base (first 2km), 4,500/km, Night surcharge 5,000
    private static final BigDecimal E_BIKE_BASE_FARE = BigDecimal.valueOf(12000);
    private static final BigDecimal E_BIKE_RATE_PER_KM = BigDecimal.valueOf(4500);
    private static final BigDecimal E_BIKE_NIGHT_SURCHARGE = BigDecimal.valueOf(5000);

    // E-Car 4S: 20,000 base (first 2km), 12,000/km, Night surcharge 15,000
    private static final BigDecimal E_CAR_4S_BASE_FARE = BigDecimal.valueOf(20000);
    private static final BigDecimal E_CAR_4S_RATE_PER_KM = BigDecimal.valueOf(12000);
    private static final BigDecimal E_CAR_4S_NIGHT_SURCHARGE = BigDecimal.valueOf(15000);

    // E-Car 7S: 25,000 base (first 2km), 14,500/km, Night surcharge 20,000
    private static final BigDecimal E_CAR_7S_BASE_FARE = BigDecimal.valueOf(25000);
    private static final BigDecimal E_CAR_7S_RATE_PER_KM = BigDecimal.valueOf(14500);
    private static final BigDecimal E_CAR_7S_NIGHT_SURCHARGE = BigDecimal.valueOf(20000);

    private static final double BASE_DISTANCE_KM = 2.0;
    public static final java.time.ZoneId VN_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    public BigDecimal calculateFare(VehicleType vehicleType, double distanceKm) {
        return calculateFare(vehicleType, distanceKm, false);
    }

    public BigDecimal calculateFare(VehicleType vehicleType, double distanceKm, java.time.Instant bookingTime) {
        boolean isNight = isNightTime(bookingTime);
        return calculateFare(vehicleType, distanceKm, isNight);
    }

    public BigDecimal calculateFare(VehicleType vehicleType, double distanceKm, boolean isNight) {
        BigDecimal baseFare;
        BigDecimal ratePerKm;
        BigDecimal nightSurcharge;

        switch (vehicleType) {
            case ELECTRIC_MOTORBIKE -> {
                baseFare = E_BIKE_BASE_FARE;
                ratePerKm = E_BIKE_RATE_PER_KM;
                nightSurcharge = E_BIKE_NIGHT_SURCHARGE;
            }
            case ELECTRIC_CAR_4SEAT -> {
                baseFare = E_CAR_4S_BASE_FARE;
                ratePerKm = E_CAR_4S_RATE_PER_KM;
                nightSurcharge = E_CAR_4S_NIGHT_SURCHARGE;
            }
            case ELECTRIC_CAR_7SEAT -> {
                baseFare = E_CAR_7S_BASE_FARE;
                ratePerKm = E_CAR_7S_RATE_PER_KM;
                nightSurcharge = E_CAR_7S_NIGHT_SURCHARGE;
            }
            default -> {
                baseFare = E_BIKE_BASE_FARE;
                ratePerKm = E_BIKE_RATE_PER_KM;
                nightSurcharge = E_BIKE_NIGHT_SURCHARGE;
            }
        }

        BigDecimal rawFare;
        if (distanceKm <= BASE_DISTANCE_KM) {
            rawFare = baseFare;
        } else {
            double extraKm = distanceKm - BASE_DISTANCE_KM;
            BigDecimal extraFare = ratePerKm.multiply(BigDecimal.valueOf(extraKm));
            rawFare = baseFare.add(extraFare);
        }

        // Apply night surcharge (22h - 06h)
        if (isNight) {
            rawFare = rawFare.add(nightSurcharge);
        }

        // Round up to nearest 1,000 VND
        return roundUpToThousand(rawFare);
    }

    public boolean isNightTime(java.time.Instant instant) {
        if (instant == null) {
            return false;
        }
        java.time.ZonedDateTime vnTime = instant.atZone(VN_ZONE);
        int hour = vnTime.getHour();
        return hour >= 22 || hour < 6;
    }

    public BigDecimal getNightSurcharge(VehicleType vehicleType) {
        return switch (vehicleType) {
            case ELECTRIC_MOTORBIKE -> E_BIKE_NIGHT_SURCHARGE;
            case ELECTRIC_CAR_4SEAT -> E_CAR_4S_NIGHT_SURCHARGE;
            case ELECTRIC_CAR_7SEAT -> E_CAR_7S_NIGHT_SURCHARGE;
        };
    }

    public BigDecimal calculateDriverIncome(BigDecimal finalFare) {
        // Platform commission 20%, Driver receives 80%
        return finalFare.multiply(BigDecimal.valueOf(0.80)).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal roundUpToThousand(BigDecimal amount) {
        long value = amount.setScale(0, RoundingMode.CEILING).longValue();
        long remainder = value % 1000;
        if (remainder != 0) {
            value += (1000 - remainder);
        }
        return BigDecimal.valueOf(value);
    }
}
