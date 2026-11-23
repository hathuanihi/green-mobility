package com.greenmobility.modules.incentive.job;

import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.modules.carbon.api.CarbonModuleApi;
import com.greenmobility.modules.incentive.domain.TripIncentiveAward;
import com.greenmobility.modules.incentive.repository.TripIncentiveAwardRepository;
import com.greenmobility.modules.incentive.service.IncentiveAwardService;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class IncentiveReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(IncentiveReconciliationJob.class);

    private final TripIncentiveAwardRepository awardRepository;
    private final IncentiveAwardService awardService;
    private final CarbonModuleApi carbonModuleApi;
    private final TripModuleApi tripModuleApi;
    private final PlatformSettingsService settingsService;

    public IncentiveReconciliationJob(TripIncentiveAwardRepository awardRepository,
                                      IncentiveAwardService awardService,
                                      CarbonModuleApi carbonModuleApi,
                                      TripModuleApi tripModuleApi,
                                      PlatformSettingsService settingsService) {
        this.awardRepository = awardRepository;
        this.awardService = awardService;
        this.carbonModuleApi = carbonModuleApi;
        this.tripModuleApi = tripModuleApi;
        this.settingsService = settingsService;
    }

    @Scheduled(fixedDelay = 120_000, initialDelay = 45_000)
    @SchedulerLock(name = "IncentiveReconciliationJob", lockAtLeastFor = "30s", lockAtMostFor = "3m")
    public void reconcileStuckPendingAwards() {
        int delayMinutes = settingsService.getIncentiveReconcileMinutes();
        Instant cutoff = Instant.now().minusSeconds(delayMinutes * 60L);

        List<TripIncentiveAward> stuck = awardRepository.findStuckPendingAwards(cutoff, PageRequest.of(0, 50));
        if (stuck.isEmpty()) {
            return;
        }

        log.info("Found {} stuck PENDING_CONDITIONS awards older than {}m, starting reconciliation",
                stuck.size(), delayMinutes);

        for (TripIncentiveAward award : stuck) {
            try {
                BigDecimal receiptCo2 = carbonModuleApi.getReceiptCo2SavedGrams(award.getTripId()).orElse(null);
                Optional<TripSnapshotDto> tripOpt = tripModuleApi.getTrip(award.getTripId());
                boolean paymentPaid = tripOpt.isPresent() && tripOpt.get().paymentStatus() == PaymentStatus.PAID;

                if (receiptCo2 != null || paymentPaid) {
                    awardService.tryReconcilePendingAward(award.getId(), receiptCo2, paymentPaid);
                }
            } catch (Exception e) {
                log.error("Failed to reconcile stuck incentive award {} for trip {}: {}",
                        award.getId(), award.getTripId(), e.getMessage());
            }
        }
    }
}
