package com.greenmobility.modules.ledger.repository;

import com.greenmobility.modules.ledger.domain.AccountType;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM LedgerAccount a WHERE a.id = :id")
    Optional<LedgerAccount> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM LedgerAccount a WHERE a.accountNumber = :accountNumber")
    Optional<LedgerAccount> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    Optional<LedgerAccount> findByOwnerIdAndAccountTypeAndCurrency(UUID ownerId, AccountType accountType, String currency);

    List<LedgerAccount> findByOwnerId(UUID ownerId);

    List<LedgerAccount> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM LedgerAccount a WHERE a.accountType = :accountType AND UPPER(a.currency) = UPPER(:currency)")
    java.math.BigDecimal sumBalanceByAccountTypeAndCurrency(@Param("accountType") AccountType accountType,
                                                            @Param("currency") String currency);

    @Query("SELECT a.id FROM LedgerAccount a WHERE (:currency IS NULL OR :currency = '' OR UPPER(a.currency) = UPPER(:currency)) AND (:ownerId IS NULL OR a.ownerId = :ownerId)")
    List<UUID> findAccountIdsByFilter(@Param("currency") String currency, @Param("ownerId") UUID ownerId);

    @Modifying
    @Query(value = "INSERT INTO ledger_accounts (account_number, owner_id, account_type, currency, balance, status, allow_negative, version) " +
            "VALUES (:accountNumber, :ownerId, :accountType, :currency, 0.0000, 'ACTIVE', :allowNegative, 0) " +
            "ON CONFLICT (account_number) DO NOTHING", nativeQuery = true)
    void insertIfNotExists(@Param("accountNumber") String accountNumber,
                           @Param("ownerId") UUID ownerId,
                           @Param("accountType") String accountType,
                           @Param("currency") String currency,
                           @Param("allowNegative") boolean allowNegative);
}
