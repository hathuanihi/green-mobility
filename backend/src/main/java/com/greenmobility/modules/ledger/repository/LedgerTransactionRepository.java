package com.greenmobility.modules.ledger.repository;

import com.greenmobility.modules.ledger.domain.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID>, JpaSpecificationExecutor<LedgerTransaction> {

    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<LedgerTransaction> findByTransactionCode(String transactionCode);

    boolean existsByReversalOf(UUID reversalOf);

    @Query("SELECT DISTINCT e.transactionId FROM LedgerEntry e WHERE e.accountId IN :accountIds")
    List<UUID> findTransactionIdsByAccountIds(@Param("accountIds") List<UUID> accountIds);
}
