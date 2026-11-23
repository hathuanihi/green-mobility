package com.greenmobility.modules.incentive.repository;

import com.greenmobility.modules.incentive.domain.Reward;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RewardRepository extends JpaRepository<Reward, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reward r WHERE r.id = :id")
    Optional<Reward> findByIdWithLock(@Param("id") UUID id);

    @Query("SELECT r FROM Reward r WHERE (:activeOnly = FALSE OR r.isActive = TRUE) " +
            "AND (:category IS NULL OR :category = '' OR r.category = :category) " +
            "ORDER BY r.createdAt DESC")
    Page<Reward> findCatalog(@Param("activeOnly") boolean activeOnly,
                             @Param("category") String category,
                             Pageable pageable);

    @Modifying
    @Query("UPDATE Reward r SET r.quantityAvailable = r.quantityAvailable - 1, r.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id AND r.isActive = TRUE AND r.quantityAvailable > 0")
    int decrementStockAtomic(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Reward r SET r.quantityAvailable = r.quantityAvailable + 1, r.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id")
    int incrementStockAtomic(@Param("id") UUID id);
}
