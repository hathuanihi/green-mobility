package com.greenmobility.modules.payment.service;

import com.greenmobility.modules.payment.domain.PlatformSetting;
import com.greenmobility.modules.payment.repository.PlatformSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlatformSettingsService {

    private static final Logger log = LoggerFactory.getLogger(PlatformSettingsService.class);

    private final PlatformSettingRepository settingRepository;
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000; // 60 seconds

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

    @Transactional
    public void updateSetting(String key, String value, UUID updatedBy) {
        PlatformSetting setting = settingRepository.findById(key)
                .orElse(new PlatformSetting(key, value, updatedBy));
        setting.setValue(value);
        setting.setUpdatedBy(updatedBy);
        setting.setUpdatedAt(Instant.now());
        settingRepository.save(setting);

        localCache.remove(key);
        log.info("Platform setting updated: key={}, value={}, by={}", key, value, updatedBy);
    }

    public Map<String, String> getAllSettings() {
        return settingRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(PlatformSetting::getKey, PlatformSetting::getValue));
    }
}

