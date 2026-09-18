package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculationService {

    // E-Bike: 12,000 base (first 2km), 4,500/km
    private static final BigDecimal E_BIKE_BASE_FARE = BigDecimal.valueOf(12000);
    private static final BigDecimal E_BIKE_RATE_PER_KM = BigDecimal.valueOf(4500);

    // E-Car 4S: 20,000 base (first 2km), 12,000/km
    private static final BigDecimal E_CAR_4S_BASE_FARE = BigDecimal.valueOf(20000);
    private static final BigDecimal E_CAR_4S_RATE_PER_KM = BigDecimal.valueOf(12000);

    // E-Car 7S: 25,000 base (first 2km), 14,500/km
    private static final BigDecimal E_CAR_7S_BASE_FARE = BigDecimal.valueOf(25000);
    private static final BigDecimal E_CAR_7S_RATE_PER_KM = BigDecimal.valueOf(14500);

    private static final double BASE_DISTANCE_KM = 2.0;

    public BigDecimal calculateFare(VehicleType vehicleType, double distanceKm) {
        BigDecimal baseFare;
        BigDecimal ratePerKm;

        switch (vehicleType) {
            case ELECTRIC_MOTORBIKE -> {
                baseFare = E_BIKE_BASE_FARE;
                ratePerKm = E_BIKE_RATE_PER_KM;
            }
            case ELECTRIC_CAR_4SEAT -> {
                baseFare = E_CAR_4S_BASE_FARE;
                ratePerKm = E_CAR_4S_RATE_PER_KM;
            }
            case ELECTRIC_CAR_7SEAT -> {
                baseFare = E_CAR_7S_BASE_FARE;
                ratePerKm = E_CAR_7S_RATE_PER_KM;
            }
            default -> {
                baseFare = E_BIKE_BASE_FARE;
                ratePerKm = E_BIKE_RATE_PER_KM;
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

        // Round up to nearest 1,000 VND
        return roundUpToThousand(rawFare);
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
