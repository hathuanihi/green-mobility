package com.greenmobility.modules.payment.api;

import com.greenmobility.modules.payment.domain.AccountType;
import com.greenmobility.modules.payment.domain.LedgerAccount;
import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.ledger.LedgerAccountRepository;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.service.PlatformSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentModuleApiImpl implements PaymentModuleApi {

    private final PaymentRepository paymentRepository;
    private final LedgerAccountRepository accountRepository;
    private final PlatformSettingsService settingsService;

    public PaymentModuleApiImpl(PaymentRepository paymentRepository,
                                LedgerAccountRepository accountRepository,
                                PlatformSettingsService settingsService) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.settingsService = settingsService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOutstandingPayment(UUID customerId) {
        return getOutstandingTripId(customerId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> getOutstandingTripId(UUID customerId) {
        Instant cutoff = Instant.now().minusSeconds(15 * 60L); // 15 mins
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> customerId.equals(p.getCustomerId()))
                .filter(p -> List.of("FAILED", "PENDING", "INITIATED").contains(p.getStatus()))
                .filter(p -> !"CASH".equalsIgnoreCase(p.getMethod()) || "FAILED".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getCreatedAt().isBefore(cutoff))
                .toList();

        if (!payments.isEmpty()) {
            return Optional.of(payments.get(0).getTripId());
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAcceptCashTrips(UUID driverUserId) {
        long debt = getCashDebtVnd(driverUserId);
        return debt <= getCashDebtLimitVnd();
    }

    @Override
    @Transactional(readOnly = true)
    public long getCashDebtVnd(UUID driverUserId) {
        Optional<LedgerAccount> acc = accountRepository.findByOwnerIdAndAccountTypeAndCurrency(
                driverUserId, AccountType.DRIVER_EARNINGS, "VND"
        );
        if (acc.isPresent() && acc.get().getBalance().longValue() < 0) {
            return Math.abs(acc.get().getBalance().longValue());
        }
        return 0L;
    }

    @Override
    public long getCashDebtLimitVnd() {
        return settingsService.getCashDebtLimitVnd();
    }
}

