package com.greenmobility.modules.ledger.repository;

import com.greenmobility.modules.ledger.domain.LedgerEntry;
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

    @Query(value = "SELECT e.* FROM ledger_entries e " +
            "JOIN ledger_transactions t ON t.id = e.transaction_id " +
            "WHERE e.account_id IN (:accountIds) " +
            "AND (CAST(:txType AS VARCHAR) IS NULL OR t.transaction_type = CAST(:txType AS VARCHAR)) " +
            "AND (CAST(:fromTime AS TIMESTAMPTZ) IS NULL OR e.created_at >= CAST(:fromTime AS TIMESTAMPTZ)) " +
            "AND (CAST(:toTime AS TIMESTAMPTZ) IS NULL OR e.created_at <= CAST(:toTime AS TIMESTAMPTZ)) " +
            "AND (CAST(:cursorTime AS TIMESTAMPTZ) IS NULL OR (e.created_at, e.id) < (CAST(:cursorTime AS TIMESTAMPTZ), CAST(:cursorId AS UUID))) " +
            "ORDER BY e.created_at DESC, e.id DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<LedgerEntry> findFilteredEntriesForAccounts(@Param("accountIds") List<UUID> accountIds,
                                                     @Param("txType") String txType,
                                                     @Param("fromTime") Instant fromTime,
                                                     @Param("toTime") Instant toTime,
                                                     @Param("cursorTime") Instant cursorTime,
                                                     @Param("cursorId") UUID cursorId,
                                                     @Param("limit") int limit);

    @Query(value = "SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0) FROM ledger_entries WHERE account_id = :accountId", nativeQuery = true)
    BigDecimal calculateBalanceForAccount(@Param("accountId") UUID accountId);

    @Query(value = "SELECT t.transaction_code FROM ledger_transactions t " +
            "JOIN ledger_entries e ON e.transaction_id = t.id " +
            "GROUP BY t.id, t.transaction_code " +
            "HAVING COALESCE(SUM(CASE WHEN e.entry_type = 'DEBIT' THEN e.amount ELSE 0 END), 0) " +
            "    <> COALESCE(SUM(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount ELSE 0 END), 0) " +
            "    OR COALESCE(SUM(CASE WHEN e.entry_type = 'DEBIT' THEN e.amount ELSE 0 END), 0) = 0", nativeQuery = true)
    List<String> findUnbalancedTransactionCodes();

    @Query(value = "SELECT t.transaction_code FROM ledger_transactions t " +
            "JOIN ledger_entries e ON e.transaction_id = t.id " +
            "JOIN ledger_accounts a ON a.id = e.account_id " +
            "GROUP BY t.id, t.transaction_code " +
            "HAVING COUNT(DISTINCT a.currency) > 1", nativeQuery = true)
    List<String> findCrossCurrencyTransactionCodes();

    @Query(value = "SELECT a.account_number, a.balance, " +
            "COALESCE(SUM(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount WHEN e.entry_type = 'DEBIT' THEN -e.amount ELSE 0 END), 0) AS computed_balance " +
            "FROM ledger_accounts a " +
            "LEFT JOIN ledger_entries e ON e.account_id = a.id " +
            "GROUP BY a.id, a.account_number, a.balance " +
            "HAVING a.balance <> COALESCE(SUM(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount WHEN e.entry_type = 'DEBIT' THEN -e.amount ELSE 0 END), 0)", nativeQuery = true)
    List<Object[]> findAccountBalanceDrifts();

    @Query(value = "SELECT currency, COALESCE(SUM(balance), 0) FROM ledger_accounts GROUP BY currency", nativeQuery = true)
    List<Object[]> sumBalancesByCurrency();
}
