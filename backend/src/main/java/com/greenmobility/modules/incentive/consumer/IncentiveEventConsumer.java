package com.greenmobility.modules.incentive.consumer;

import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.carbon.event.CarbonCalculatedEvent;
import com.greenmobility.modules.incentive.service.IncentiveAwardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class IncentiveEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncentiveEventConsumer.class);

    private final IncentiveAwardService awardService;

    public IncentiveEventConsumer(IncentiveAwardService awardService) {
        this.awardService = awardService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CARBON_CALCULATED)
    public void handleCarbonCalculated(CarbonCalculatedEvent event) {
        log.info("Incentive consumer received CarbonCalculatedEvent: tripId={}, co2={}g",
                event.getTripId(), event.getCo2SavedGrams());
        awardService.onCarbonCalculated(
                event.getTripId(),
                event.getCustomerId(),
                event.getDriverUserId(),
                event.getCo2SavedGrams()
        );
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_SUCCEEDED_INCENTIVE)
    public void handlePaymentSucceeded(Map<String, Object> payload) {
        Object tripIdRaw = payload != null ? payload.get("tripId") : null;
        if (tripIdRaw == null || String.valueOf(tripIdRaw).isBlank()) {
            log.warn("PaymentSucceeded payload missing tripId: {}", payload);
            return;
        }
        UUID tripId = UUID.fromString(String.valueOf(tripIdRaw));
        log.info("Incentive consumer received payment.event.succeeded for tripId={}", tripId);
        awardService.onPaymentSucceeded(tripId);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_REFUNDED_INCENTIVE)
    public void handlePaymentRefunded(Map<String, Object> payload) {
        Object tripIdRaw = payload != null ? payload.get("tripId") : null;
        if (tripIdRaw == null || String.valueOf(tripIdRaw).isBlank()) {
            log.warn("PaymentRefunded payload missing tripId: {}", payload);
            return;
        }
        UUID tripId = UUID.fromString(String.valueOf(tripIdRaw));
        boolean fullRefund = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("fullRefund", "false")));
        String reason = payload.get("reason") != null ? String.valueOf(payload.get("reason")) : "";
        log.info("Incentive consumer received payment.event.refunded for tripId={}, fullRefund={}", tripId, fullRefund);
        awardService.onPaymentRefunded(tripId, fullRefund, reason);
    }
}
