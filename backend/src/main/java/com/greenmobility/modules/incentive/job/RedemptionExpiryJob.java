package com.greenmobility.modules.incentive.job;

import com.greenmobility.modules.incentive.domain.RewardRedemption;
import com.greenmobility.modules.incentive.repository.RewardRedemptionRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class RedemptionExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(RedemptionExpiryJob.class);

    private final RewardRedemptionRepository redemptionRepository;

    public RedemptionExpiryJob(RewardRedemptionRepository redemptionRepository) {
        this.redemptionRepository = redemptionRepository;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @SchedulerLock(name = "RedemptionExpiryJob", lockAtLeastFor = "30s", lockAtMostFor = "4m")
    @Transactional
    public void expireStaleRedemptions() {
        List<RewardRedemption> expired = redemptionRepository.findExpiredAvailableRedemptions(
                Instant.now(), PageRequest.of(0, 100));
        if (expired.isEmpty()) {
            return;
        }
        for (RewardRedemption r : expired) {
            r.setStatus("EXPIRED");
        }
        redemptionRepository.saveAll(expired);
        log.info("Expired {} stale AVAILABLE/ISSUED redemptions", expired.size());
    }
}
