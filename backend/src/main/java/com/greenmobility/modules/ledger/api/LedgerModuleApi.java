package com.greenmobility.modules.ledger.api;

import com.greenmobility.modules.ledger.domain.AccountType;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import com.greenmobility.modules.ledger.domain.LedgerTransaction;

import java.util.Optional;
import java.util.UUID;

public interface LedgerModuleApi {

    LedgerTransaction post(PostingRequest request);

    LedgerAccount getOrCreateUserWallet(UUID userId);

    LedgerAccount getOrCreateDriverEarningsAccount(UUID driverUserId);

    LedgerAccount getOrCreateCarbonWallet(UUID userId);

    LedgerAccount getOrCreatePointsWallet(UUID userId);

    void provisionUserWallets(UUID userId, boolean isDriver);

    Optional<LedgerAccount> findAccount(UUID ownerId, AccountType accountType, String currency);

    LedgerAccount getSystemAccount(AccountType accountType);

    LedgerAccount getSystemAccount(AccountType accountType, String currency);

    LedgerTransaction reverseTransaction(UUID adminId, String adminEmail, UUID transactionId, String reason, String ipAddress);

    LedgerAccount freezeUserCarbonWallets(UUID userId, UUID adminId, String adminEmail, String reason, String ipAddress);

    LedgerAccount unfreezeUserCarbonWallets(UUID userId, UUID adminId, String adminEmail, String reason, String ipAddress);
}
