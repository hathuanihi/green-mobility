package com.greenmobility.modules.incentive.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "partner_name", nullable = false, length = 100)
    private String partnerName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "carbon_credit_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal carbonCreditCost = BigDecimal.ZERO;

    @Column(name = "loyalty_point_cost", nullable = false)
    private Integer loyaltyPointCost = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "discount_type", length = 20)
    private String discountType;

    @Column(name = "discount_value")
    private Long discountValue;

    @Column(name = "max_discount_vnd")
    private Long maxDiscountVnd;

    @Column(name = "min_fare_vnd", nullable = false)
    private Long minFareVnd = 0L;

    @Column(name = "validity_days", nullable = false)
    private Integer validityDays = 30;

    @Column(name = "max_redemptions_per_user", nullable = false)
    private Integer maxRedemptionsPerUser = 5;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getCarbonCreditCost() { return carbonCreditCost; }
    public void setCarbonCreditCost(BigDecimal carbonCreditCost) { this.carbonCreditCost = carbonCreditCost; }

    public Integer getLoyaltyPointCost() { return loyaltyPointCost; }
    public void setLoyaltyPointCost(Integer loyaltyPointCost) { this.loyaltyPointCost = loyaltyPointCost; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public Long getDiscountValue() { return discountValue; }
    public void setDiscountValue(Long discountValue) { this.discountValue = discountValue; }

    public Long getMaxDiscountVnd() { return maxDiscountVnd; }
    public void setMaxDiscountVnd(Long maxDiscountVnd) { this.maxDiscountVnd = maxDiscountVnd; }

    public Long getMinFareVnd() { return minFareVnd; }
    public void setMinFareVnd(Long minFareVnd) { this.minFareVnd = minFareVnd; }

    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }

    public Integer getMaxRedemptionsPerUser() { return maxRedemptionsPerUser; }
    public void setMaxRedemptionsPerUser(Integer maxRedemptionsPerUser) { this.maxRedemptionsPerUser = maxRedemptionsPerUser; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
