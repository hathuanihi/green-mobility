package com.greenmobility.modules.carbon.listener;

import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.carbon.service.CarbonCalculationService;
import com.greenmobility.modules.trip.event.TripCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TripCompletedCarbonConsumer {

    private static final Logger log = LoggerFactory.getLogger(TripCompletedCarbonConsumer.class);

    private final CarbonCalculationService carbonCalculationService;

    public TripCompletedCarbonConsumer(CarbonCalculationService carbonCalculationService) {
        this.carbonCalculationService = carbonCalculationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRIP_COMPLETED)
    public void handleTripCompleted(TripCompletedEvent event) {
        log.info("[TripCompletedCarbonConsumer] Received TripCompletedEvent: tripId={}, code={}, actualDistance={}m",
                event.getTripId(), event.getTripCode(), event.getActualDistanceM());

        try {
            carbonCalculationService.processTripCompletion(event);
        } catch (Exception e) {
            log.error("[TripCompletedCarbonConsumer] Error calculating carbon for trip {}: {}",
                    event.getTripId(), e.getMessage(), e);
        }
    }
}
