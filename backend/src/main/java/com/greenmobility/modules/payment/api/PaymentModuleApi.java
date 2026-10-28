package com.greenmobility.modules.payment.api;

import java.util.Optional;
import java.util.UUID;

public interface PaymentModuleApi {

    boolean hasOutstandingPayment(UUID customerId);

    Optional<UUID> getOutstandingTripId(UUID customerId);

    boolean canAcceptCashTrips(UUID driverUserId);

    long getCashDebtVnd(UUID driverUserId);

    long getCashDebtLimitVnd();
}

