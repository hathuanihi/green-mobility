package com.greenmobility.modules.incentive.repository;

import com.greenmobility.modules.incentive.domain.CarbonConversion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface CarbonConversionRepository extends JpaRepository<CarbonConversion, UUID> {

    @Query("SELECT COALESCE(SUM(c.pccAmount), 0) FROM CarbonConversion c " +
            "WHERE c.userId = :userId AND c.status = 'COMPLETED' AND c.createdAt >= :since")
    BigDecimal sumConvertedPccSince(@Param("userId") UUID userId, @Param("since") Instant since);

    Page<CarbonConversion> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
