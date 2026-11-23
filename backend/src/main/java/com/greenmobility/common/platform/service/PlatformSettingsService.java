package com.greenmobility.common.platform.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.domain.PlatformSetting;
import com.greenmobility.common.platform.repository.PlatformSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PlatformSettingsService {

    private static final Logger log = LoggerFactory.getLogger(PlatformSettingsService.class);

    private final PlatformSettingRepository settingRepository;
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000;

    private record CacheEntry(String value, long expiresAt) {}

    public PlatformSettingsService(PlatformSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public String getSetting(String key, String defaultValue) {
        long now = System.currentTimeMillis();
        CacheEntry cached = localCache.get(key);
        if (cached != null && cached.expiresAt > now) {
            return cached.value;
        }

        String value = settingRepository.findById(key)
                .map(PlatformSetting::getValue)
                .orElse(defaultValue);

        localCache.put(key, new CacheEntry(value, now + CACHE_TTL_MS));
        return value;
    }

    // --- Sprint 5 Payment Settings ---
    public BigDecimal getCommissionRate() {
        return new BigDecimal(getSetting("commission_rate", "0.20"));
    }

    public long getTopupMinVnd() {
        return Long.parseLong(getSetting("topup_min_vnd", "10000"));
    }

    public long getTopupMaxVnd() {
        return Long.parseLong(getSetting("topup_max_vnd", "10000000"));
    }

    public long getWithdrawMinVnd() {
        return Long.parseLong(getSetting("withdraw_min_vnd", "50000"));
    }

    public long getWithdrawMaxVnd() {
        return Long.parseLong(getSetting("withdraw_max_vnd", "20000000"));
    }

    public long getWithdrawDailyLimitVnd() {
        return Long.parseLong(getSetting("withdraw_daily_limit_vnd", "20000000"));
    }

    public long getCashDebtLimitVnd() {
        return Long.parseLong(getSetting("cash_debt_limit_vnd", "200000"));
    }

    public int getPaymentExpireMinutes() {
        return Integer.parseInt(getSetting("payment_expire_minutes", "15"));
    }

    public int getCashAutoConfirmMinutes() {
        return Integer.parseInt(getSetting("cash_auto_confirm_minutes", "30"));
    }

    // --- Sprint 6 Incentive & Carbon Settings ---
    public BigDecimal getPccGramsPerUnit() {
        return new BigDecimal(getSetting("pcc_grams_per_unit", "1000"));
    }

    public BigDecimal getPccCustomerShareRatio() {
        return new BigDecimal(getSetting("pcc_customer_share_ratio", "0.70"));
    }

    public BigDecimal getEcoPointGramsPerPoint() {
        return new BigDecimal(getSetting("eco_point_grams_per_point", "100"));
    }

    public BigDecimal getDriverEcoPointRatio() {
        return new BigDecimal(getSetting("driver_eco_point_ratio", "0.50"));
    }

    public long getPccToVndRate() {
        return Long.parseLong(getSetting("pcc_to_vnd_rate", "500"));
    }

    public BigDecimal getPccConvertMin() {
        return new BigDecimal(getSetting("pcc_convert_min", "0.5000"));
    }

    public BigDecimal getPccConvertMonthlyCap() {
        return new BigDecimal(getSetting("pcc_convert_monthly_cap", "100.0000"));
    }

    public boolean isRedemptionsFrozen() {
        return Boolean.parseBoolean(getSetting("redemptions_frozen", "false"));
    }

    public int getIncentiveReconcileMinutes() {
        return Integer.parseInt(getSetting("incentive_reconcile_minutes", "5"));
    }

    @Transactional
    public void updateSetting(String key, String value, UUID updatedBy) {
        validateSettingValue(key, value);
        PlatformSetting setting = settingRepository.findById(key)
                .orElse(new PlatformSetting(key, value, updatedBy));
        setting.setValue(value.trim());
        setting.setUpdatedBy(updatedBy);
        setting.setUpdatedAt(Instant.now());
        settingRepository.save(setting);

        localCache.remove(key);
        log.info("Platform setting updated: key={}, value={}, by={}", key, value, updatedBy);
    }

    private void validateSettingValue(String key, String rawValue) {
        if (key == null || key.isBlank() || rawValue == null || rawValue.isBlank()) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "SETTING_INVALID", "Tham số cấu hình không hợp lệ");
        }
        String val = rawValue.trim();
        try {
            switch (key) {
                case "commission_rate" -> {
                    BigDecimal bd = new BigDecimal(val);
                    if (bd.compareTo(BigDecimal.ZERO) < 0 || bd.compareTo(new BigDecimal("0.50")) > 0) {
                        throw new IllegalArgumentException("commission_rate phải nằm trong [0.00, 0.50]");
                    }
                }
                case "pcc_customer_share_ratio" -> {
                    BigDecimal bd = new BigDecimal(val);
                    if (bd.compareTo(BigDecimal.ZERO) < 0 || bd.compareTo(BigDecimal.ONE) > 0) {
                        throw new IllegalArgumentException("pcc_customer_share_ratio phải nằm trong [0.00, 1.00]");
                    }
                }
                case "driver_eco_point_ratio" -> {
                    BigDecimal bd = new BigDecimal(val);
                    if (bd.compareTo(BigDecimal.ZERO) < 0 || bd.compareTo(new BigDecimal("2.00")) > 0) {
                        throw new IllegalArgumentException("driver_eco_point_ratio phải nằm trong [0.00, 2.00]");
                    }
                }
                case "pcc_grams_per_unit" -> {
                    BigDecimal bd = new BigDecimal(val);
                    if (bd.compareTo(new BigDecimal("100")) < 0 || bd.compareTo(new BigDecimal("1000000")) > 0) {
                        throw new IllegalArgumentException("pcc_grams_per_unit phải nằm trong [100, 1000000]");
                    }
                }
                case "eco_point_grams_per_point" -> {
                    BigDecimal bd = new BigDecimal(val);
                    if (bd.compareTo(BigDecimal.ONE) < 0 || bd.compareTo(new BigDecimal("10000")) > 0) {
                        throw new IllegalArgumentException("eco_point_grams_per_point phải nằm trong [1, 10000]");
                    }
                }
                case "pcc_to_vnd_rate" -> {
                    long rate = Long.parseLong(val);
                    if (rate < 100 || rate > 50000) {
                        throw new IllegalArgumentException("pcc_to_vnd_rate phải nằm trong [100, 50000]");
                    }
                }
                case "pcc_convert_min" -> {
                    BigDecimal bd = new BigDecimal(val);
                    if (bd.compareTo(new BigDecimal("0.0100")) < 0) {
                        throw new IllegalArgumentException("pcc_convert_min phải >= 0.0100");
                    }
                }
                case "pcc_convert_monthly_cap" -> {
                    BigDecimal bd = new BigDecimal(val);
                    if (bd.compareTo(getPccConvertMin()) < 0) {
                        throw new IllegalArgumentException("pcc_convert_monthly_cap phải >= pcc_convert_min");
                    }
                }
                case "redemptions_frozen" -> {
                    if (!"true".equalsIgnoreCase(val) && !"false".equalsIgnoreCase(val)) {
                        throw new IllegalArgumentException("redemptions_frozen phải là true hoặc false");
                    }
                }
                case "incentive_reconcile_minutes" -> {
                    int mins = Integer.parseInt(val);
                    if (mins < 1 || mins > 1440) {
                        throw new IllegalArgumentException("incentive_reconcile_minutes phải trong [1, 1440]");
                    }
                }
                default -> {
                    // Validate numeric positive for remaining Sprint 5 settings
                    if (key.endsWith("_vnd") || key.endsWith("_minutes")) {
                        long num = Long.parseLong(val);
                        if (num < 0) throw new IllegalArgumentException(key + " không được âm");
                    }
                }
            }
        } catch (PaymentBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "SETTING_INVALID",
                    "Giá trị cấu hình không hợp lệ cho " + key + ": " + e.getMessage());
        }
    }

    public void evictCache(String key) {
        localCache.remove(key);
    }

    public Map<String, String> getAllSettings() {
        return settingRepository.findAll().stream()
                .collect(Collectors.toMap(PlatformSetting::getKey, PlatformSetting::getValue));
    }
}
