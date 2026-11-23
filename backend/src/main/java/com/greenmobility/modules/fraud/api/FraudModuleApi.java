package com.greenmobility.modules.fraud.api;

import java.util.Optional;
import java.util.UUID;

public interface FraudModuleApi {

    boolean hasBlockingFraudAlert(UUID tripId);

    Optional<String> getBlockingFraudReason(UUID tripId);
}
