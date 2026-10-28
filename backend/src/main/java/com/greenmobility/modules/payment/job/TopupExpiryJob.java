package com.greenmobility.modules.payment.job;

import com.greenmobility.modules.payment.domain.TopupRequest;
import com.greenmobility.modules.payment.repository.TopupRequestRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class TopupExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(TopupExpiryJob.class);

    private final TopupRequestRepository topupRepository;

    public TopupExpiryJob(TopupRequestRepository topupRepository) {
        this.topupRepository = topupRepository;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000) // 5 minutes
    @SchedulerLock(name = "TopupExpiryJob", lockAtLeastFor = "1m", lockAtMostFor = "10m")
    @Transactional
    public void expirePendingTopups() {
        Instant now = Instant.now();
        List<TopupRequest> expired = topupRepository.findExpiredPendingTopups(now);

        for (TopupRequest topup : expired) {
            topup.setStatus("EXPIRED");
            topupRepository.save(topup);
            log.info("Topup request {} marked as EXPIRED", topup.getId());
        }
    }
}

