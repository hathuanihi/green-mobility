package com.greenmobility.modules.carbon.repository;

import com.greenmobility.modules.carbon.entity.EmissionFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmissionFactorRepository extends JpaRepository<EmissionFactor, UUID> {

    @Query("SELECT ef FROM EmissionFactor ef WHERE ef.vehicleCategory = :category " +
           "AND ef.isActive = true " +
           "AND ef.effectiveFrom <= :targetDate " +
           "AND (ef.effectiveTo IS NULL OR ef.effectiveTo >= :targetDate) " +
           "ORDER BY ef.effectiveFrom DESC")
    List<EmissionFactor> findValidFactorsForDate(
            @Param("category") String category,
            @Param("targetDate") LocalDate targetDate);

    default Optional<EmissionFactor> findActiveFactor(String category, LocalDate targetDate) {
        List<EmissionFactor> list = findValidFactorsForDate(category, targetDate);
        if (!list.isEmpty()) {
            return Optional.of(list.get(0));
        }
        // Fallback: any active factor for this category
        return findFirstByVehicleCategoryAndIsActiveTrueOrderByEffectiveFromDesc(category);
    }

    Optional<EmissionFactor> findFirstByVehicleCategoryAndIsActiveTrueOrderByEffectiveFromDesc(String category);

    List<EmissionFactor> findByVehicleCategory(String vehicleCategory);

    List<EmissionFactor> findByIsActiveTrue();
}
