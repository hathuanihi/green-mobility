package com.greenmobility.modules.payment.ledger;

import com.greenmobility.modules.payment.domain.LedgerEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    @Query("SELECT e FROM LedgerEntry e WHERE e.accountId = :accountId ORDER BY e.createdAt DESC, e.id DESC")
    List<LedgerEntry> findByAccountIdRecent(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("SELECT e FROM LedgerEntry e WHERE e.accountId = :accountId AND e.createdAt < :cursor ORDER BY e.createdAt DESC, e.id DESC")
    List<LedgerEntry> findByAccountIdWithCursor(@Param("accountId") UUID accountId, @Param("cursor") Instant cursor, Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0) FROM ledger_entries WHERE account_id = :accountId", nativeQuery = true)
    BigDecimal calculateBalanceForAccount(@Param("accountId") UUID accountId);
}

