package com.greenmobility.modules.payment.job;

import com.greenmobility.modules.payment.repository.IdempotencyKeyRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyKeyRepository repository;

    public IdempotencyCleanupJob(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @SchedulerLock(name = "IdempotencyCleanupJob", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    @Transactional
    public void cleanupOldKeys() {
        Instant cutoff = Instant.now().minusSeconds(48 * 3600L); // 48 hours
        int deleted = repository.deleteKeysOlderThan(cutoff);
        log.info("Cleaned up {} expired idempotency keys (older than 48 hours)", deleted);
    }
}

