package com.greenmobility.modules.payment.repository;

import com.greenmobility.modules.payment.domain.IdempotencyKeyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, IdempotencyKeyRecord.IdempotencyKeyId> {

    Optional<IdempotencyKeyRecord> findByUserIdAndEndpointAndKey(UUID userId, String endpoint, String key);

    @Modifying
    @Query("DELETE FROM IdempotencyKeyRecord r WHERE r.createdAt < :cutoff")
    int deleteKeysOlderThan(@Param("cutoff") Instant cutoff);
}

