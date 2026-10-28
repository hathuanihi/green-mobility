package com.greenmobility.modules.payment.consumer;

import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.payment.service.PaymentService;
import com.greenmobility.modules.trip.event.TripCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TripCompletedPaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(TripCompletedPaymentConsumer.class);

    private final PaymentService paymentService;

    public TripCompletedPaymentConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRIP_COMPLETED_PAYMENT)
    public void onTripCompleted(TripCompletedEvent event) {
        log.info("[TripCompletedPaymentConsumer] Received TripCompletedEvent: tripId={}, fare={}, method={}",
                event.getTripId(), event.getFinalAmountVnd(), event.getPaymentMethod());

        try {
            paymentService.initiatePaymentForTrip(event);
        } catch (Exception e) {
            log.error("[TripCompletedPaymentConsumer] Error processing trip payment {}: {}",
                    event.getTripId(), e.getMessage(), e);
            throw e; // RabbitMQ retry & DLQ mechanism
        }
    }
}

