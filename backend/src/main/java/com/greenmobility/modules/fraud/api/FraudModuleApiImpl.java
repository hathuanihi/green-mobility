package com.greenmobility.modules.fraud.api;

import com.greenmobility.modules.fraud.repository.FraudAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FraudModuleApiImpl implements FraudModuleApi {

    private static final List<String> BLOCKING_STATUSES = List.of("PENDING", "INVESTIGATING", "CONFIRMED_FRAUD_BANNED");

    private final FraudAlertRepository fraudAlertRepository;

    public FraudModuleApiImpl(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBlockingFraudAlert(UUID tripId) {
        if (tripId == null) return false;
        return fraudAlertRepository.existsByTripIdAndResolutionStatusIn(tripId, BLOCKING_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getBlockingFraudReason(UUID tripId) {
        if (!hasBlockingFraudAlert(tripId)) return Optional.empty();
        return Optional.of("Chuyến đi có cảnh báo gian lận đang chờ/xác nhận kiểm tra");
    }
}
