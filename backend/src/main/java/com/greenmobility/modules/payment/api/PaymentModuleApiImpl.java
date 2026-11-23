package com.greenmobility.modules.payment.api;

import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.modules.ledger.domain.AccountType;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        Instant cutoff = Instant.now().minusSeconds(15 * 60L);
        return paymentRepository.findFirstOutstandingTripId(customerId, cutoff);
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
